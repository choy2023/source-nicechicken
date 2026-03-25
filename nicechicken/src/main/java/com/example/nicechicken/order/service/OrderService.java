package com.example.nicechicken.order.service;

import com.example.nicechicken.order.dto.AdminOrderResponse;
import com.example.nicechicken.order.dto.OrderCreateRequest;
import com.example.nicechicken.order.dto.OrderStatusHistoryResponse;
import com.example.nicechicken.order.dto.UserOrderResponse;
import com.example.nicechicken.order.entity.OrderEntity;
import com.example.nicechicken.order.entity.OrderItemEntity;
import com.example.nicechicken.order.entity.OrderStatus;
import com.example.nicechicken.order.repository.OrderStatusHistoryRepository;
import com.example.nicechicken.product.entity.Product;
import com.example.nicechicken.product.entity.ProductOption;
import com.example.nicechicken.order.repository.OrderRepository;
import com.example.nicechicken.product.service.ProductService;
import com.example.nicechicken.user.service.UserService;
import com.example.nicechicken.order.event.OrderCancelledEvent;
import com.example.nicechicken.payment.event.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.example.nicechicken.user.entity.UserEntity;
import com.example.nicechicken.order.event.OrderStatusChangedEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

        private final UserService userService;
        private final ProductService productService;
        private final OrderRepository orderRepository;
        private final OrderStatusHistoryRepository orderStatusHistoryRepository;
        private final OrderStatusHistoryService orderStatusHistoryService;
        private final ApplicationEventPublisher eventPublisher;

        @EventListener
        @Transactional
        public void handlePaymentSuccess(PaymentSuccessEvent event) {
                log.info("📦 Order Module: Payment completion event received. Order ID: {}", event.orderId());
                try {
                        updateOrderStatus(event.orderId(), OrderStatus.PAID);
                } catch (IllegalStateException e) {
                        // Race condition: order was cancelled before webhook arrived.
                        // State machine blocks CANCELLED → PAID. Log and let webhook return 200.
                        log.warn("⚠️ Ignoring payment event for non-PENDING order {}: {}",
                                        event.orderId(), e.getMessage());
                }
        }

        @Transactional
        public void cancelOrder(UUID orderId, String email) {
                OrderEntity order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("Order not found."));

                // Check ownership (Check if the logged-in user is the owner of this order)
                if (order.getUser() == null || !order.getUser().getEmail().equals(email)) {
                        log.warn("🚨 Unauthorized order cancellation attempt. Order ID: {}, User Email: {}", orderId,
                                        email);
                        throw new IllegalArgumentException("You are not authorized to cancel this order.");
                }

                // Cancellation is only possible in PENDING or PAID status (not possible for
                // COOKING or above)
                if (!List.of(OrderStatus.PENDING, OrderStatus.PAID).contains(order.getStatus())) {
                        throw new IllegalStateException("Status cannot be cancelled: " + order.getStatus());
                }

                // Reuse updateOrderStatus to ensure OrderStatusChangedEvent and
                // OrderCancelledEvent (stock restoration) are published.
                updateOrderStatus(orderId, OrderStatus.CANCELLED);
                log.info("🚫 Order cancellation logic executed via updateOrderStatus: {}", orderId);
        }

        @Transactional(readOnly = true)
        public List<UserOrderResponse> getMyOrders(String email) {
                UserEntity user = userService.getUserByEmail(email);

                return orderRepository.findAllByUserIdWithItems(user.getId()).stream()
                                .map(order -> new UserOrderResponse(
                                                order.getId(),
                                                order.getTotalAmount(),
                                                order.getStatus(),
                                                order.getOrderItems().stream()
                                                                .map(item -> new UserOrderResponse.OrderItemDetail(
                                                                                item.getProductId(),
                                                                                item.getQuantity(),
                                                                                item.getPriceAtOrder(),
                                                                                item.getSelectedOptions()))
                                                                .toList()))
                                .toList();
        }

        @Transactional
        public UUID createOrder(OrderCreateRequest request, String email) {

                BigDecimal calculatedTotal = BigDecimal.ZERO;
                List<OrderItemEntity> orderItems = new ArrayList<>();

                // Sort by productId to guarantee consistent lock ordering → prevents deadlocks
                List<OrderCreateRequest.OrderItemRequest> sortedItems = request.items().stream()
                                .sorted(java.util.Comparator.comparingLong(OrderCreateRequest.OrderItemRequest::productId))
                                .toList();

                for (OrderCreateRequest.OrderItemRequest itemReq : sortedItems) {
                        Product product = productService.getProductById(itemReq.productId());

                        BigDecimal itemPrice = product.getBasePrice();

                        // [Stock Deduction] Deduct immediately upon order creation (PENDING status)
                        productService.decreaseStock(product.getId(), itemReq.quantity());

                        // Option validation and JSON data generation
                        List<Map<String, Object>> selectedOptionsJson = new ArrayList<>();
                        for (OrderCreateRequest.SelectedOptionRequest optReq : itemReq.selectedOptions()) {
                                ProductOption option = productService.getProductOption(
                                                product.getId(), optReq.optionGroup(),
                                                optReq.optionName());

                                itemPrice = itemPrice.add(option.getExtraPrice());

                                // Construct data to be stored in JSONB
                                selectedOptionsJson.add(Map.of(
                                                "group", option.getOptionGroup(),
                                                "name", option.getOptionName(),
                                                "price", option.getExtraPrice()));
                        }

                        BigDecimal itemTotal = itemPrice.multiply(BigDecimal.valueOf(itemReq.quantity()));
                        calculatedTotal = calculatedTotal.add(itemTotal);

                        // Create order detail entity
                        orderItems.add(OrderItemEntity.builder()
                                        .productId(product.getId())
                                        .quantity(itemReq.quantity())
                                        .priceAtOrder(itemPrice) // Unit price at the time of order
                                        .selectedOptions(selectedOptionsJson)
                                        .build());
                }

                // If even 1 cent is different, reject the request
                if (calculatedTotal.compareTo(request.clientTotalAmount()) != 0) {
                        throw new IllegalArgumentException(
                                        "Suspected price manipulation! Calculated: " + calculatedTotal + ", Requested: "
                                                        + request.clientTotalAmount());
                }

                UserEntity orderUser = null;
                if (email != null) {
                        orderUser = userService.findByEmail(email).orElse(null);
                }

                // Validation passed! Save now
                OrderEntity order = OrderEntity.builder()
                                .user(orderUser)
                                .customerName(request.customerName())
                                .customerPhone(request.customerPhone())
                                .customerEmail(request.customerEmail())
                                .totalAmount(calculatedTotal)
                                .build();

                // Set bidirectional relationship
                orderItems.forEach(order::addOrderItem);

                // Thanks to CascadeType.ALL, saving the order also saves the order_items
                OrderEntity savedOrder = orderRepository.save(order);

                return savedOrder.getId();
        }

        // Added to service/OrderService.java

        @Transactional(readOnly = true)
        public Page<AdminOrderResponse> getAllOrders(Pageable pageable) {
                return orderRepository.findAll(pageable)
                                .map(order -> new AdminOrderResponse(
                                                order.getId(),
                                                order.getCustomerName(),
                                                order.getCustomerPhone(),
                                                order.getTotalAmount(),
                                                order.getStatus(),
                                                order.getOrderItems().stream()
                                                                .map(item -> new AdminOrderResponse.OrderItemDetail(
                                                                                item.getProductId(),
                                                                                item.getQuantity(),
                                                                                item.getPriceAtOrder(),
                                                                                item.getSelectedOptions()))
                                                                .toList()));
        }

        @Transactional(readOnly = true)
        public List<OrderItemEntity> getOrderItemsByOrderId(UUID orderId) {
                OrderEntity order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("Order not found."));
                return order.getOrderItems();
        }

        @Transactional
        public void updateOrderStatus(UUID orderId, OrderStatus newStatus) {
                OrderEntity order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("Order not found. ID: " + orderId));

                OrderStatus oldStatus = order.getStatus();
                order.changeStatus(newStatus);

                // Record status change history (separate transaction — does not affect main flow)
                orderStatusHistoryService.record(orderId, oldStatus, newStatus, resolveChangedBy());

                // Publish status change event
                eventPublisher.publishEvent(new OrderStatusChangedEvent(
                                orderId, oldStatus, newStatus, order.getCustomerEmail()));

                // Publish additional stock restoration event upon cancellation
                if (OrderStatus.CANCELLED == newStatus) {
                        List<OrderCancelledEvent.CancelledItem> cancelledItems = order.getOrderItems().stream()
                                        .map(item -> new OrderCancelledEvent.CancelledItem(item.getProductId(),
                                                        item.getQuantity()))
                                        .toList();
                        eventPublisher.publishEvent(new OrderCancelledEvent(orderId, cancelledItems));
                }
        }

        @Transactional(readOnly = true)
        public List<OrderStatusHistoryResponse> getOrderHistory(UUID orderId) {
                return orderStatusHistoryRepository.findByOrderIdOrderByChangedAtAsc(orderId).stream()
                                .map(h -> new OrderStatusHistoryResponse(
                                                h.getOldStatus(), h.getNewStatus(),
                                                h.getChangedAt(), h.getChangedBy()))
                                .toList();
        }

        private String resolveChangedBy() {
                return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                                .filter(auth -> auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()))
                                .map(Authentication::getName)
                                .orElse("SYSTEM");
        }

        @Transactional
        public void hideOrder(UUID orderId, String email) {
                OrderEntity order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("Order not found."));

                if (order.getUser() == null || !order.getUser().getEmail().equals(email)) {
                        throw new IllegalArgumentException("You are not authorized to hide this order.");
                }

                order.softDelete();
        }
}