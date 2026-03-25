package com.example.nicechicken.order.service;

import com.example.nicechicken.order.entity.OrderEntity;
import com.example.nicechicken.order.entity.OrderStatus;
import com.example.nicechicken.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCleanupScheduler {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void deleteOldHiddenOrders() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        orderRepository.deleteByDeletedAtBefore(thirtyDaysAgo);
    }

    @Scheduled(fixedRate = 60000)
    public void cancelExpiredPendingOrders() {
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(20);
        List<OrderEntity> expiredOrders = orderRepository.findPendingOrdersBefore(OrderStatus.PENDING, thirtyMinutesAgo);

        for (OrderEntity order : expiredOrders) {
            try {
                log.info("Order expired. Cancelling order: {}", order.getId());
                orderService.updateOrderStatus(order.getId(), OrderStatus.CANCELLED);
            } catch (Exception e) {
                log.error("Error occurred while cancelling order: {}", order.getId(), e);
            }
        }
    }
}