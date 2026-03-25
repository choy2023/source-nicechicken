package com.example.nicechicken.product.service;

import com.example.nicechicken.order.event.OrderCancelledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventListener {

    private final ProductService productService;

    /**
     * Restores stock when an order is cancelled.
     *
     * Runs in the SAME transaction as OrderService.updateOrderStatus().
     * If stock restoration fails, the entire cancellation rolls back.
     * This is intentional — we must not cancel an order without restoring stock,
     * otherwise inventory counts become permanently incorrect.
     *
     * No try-catch here: swallowing exceptions would trigger Spring's
     * "rollback-only" trap (UnexpectedRollbackException), where the inner
     * @Transactional marks the global tx for rollback, but the outer method
     * tries to commit — causing a confusing runtime error.
     */
    @EventListener
    @Transactional
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("📦 Product Module: Order cancellation event received. Starting stock restoration. Order ID: {}", event.orderId());

        for (OrderCancelledEvent.CancelledItem item : event.items()) {
            log.info("🔄 Stock Restoration: Product ID {}, Quantity {}", item.productId(), item.quantity());
            productService.restoreStock(item.productId(), item.quantity());
        }

        log.info("✅ Stock restoration for all products completed. Order ID: {}", event.orderId());
    }
}
