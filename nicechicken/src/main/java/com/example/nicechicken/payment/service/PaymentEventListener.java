package com.example.nicechicken.payment.service;

import com.example.nicechicken.order.event.OrderCancelledEvent;
import com.example.nicechicken.payment.event.RefundCompletedEvent;
import com.example.nicechicken.payment.repository.PaymentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Handles refund AFTER the cancel transaction commits, in a separate thread.
     *
     * Why @TransactionalEventListener(AFTER_COMMIT) + @Async?
     * - AFTER_COMMIT: Only triggers if cancelOrder() actually committed.
     *   If the cancel transaction rolls back, we don't accidentally refund.
     * - @Async: Runs on a separate thread so the Stripe HTTP call
     *   doesn't hold a DB connection from the HikariCP pool.
     *   Without this, 10 concurrent cancellations would exhaust the
     *   default pool (10 connections), blocking all other DB operations.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("💸 Payment Module: Order cancellation event received. Starting refund process. Order ID: {}", event.orderId());

        paymentRepository.findByOrderId(event.orderId()).ifPresent(payment -> {
            if ("PAID".equals(payment.getStatus())) {
                try {
                    // Execute Stripe refund (external HTTP call — now isolated from cancel transaction)
                    RefundCreateParams params = RefundCreateParams.builder()
                            .setPaymentIntent(payment.getStripePaymentIntentId())
                            .build();

                    Refund.create(params);

                    // Update DB status
                    payment.markAsRefunded();
                    log.info("✅ Stripe refund successful and DB update completed: {}", event.orderId());

                    // Publish refund completed event
                    eventPublisher.publishEvent(new RefundCompletedEvent(event.orderId()));

                } catch (StripeException e) {
                    log.error("🚨 Stripe refund failed! Order ID: {}, Reason: {}", event.orderId(), e.getMessage());
                    // TODO: Add retry mechanism or dead letter queue for failed refunds
                }
            } else {
                log.info("ℹ️ Order already refunded or not paid. Status: {}", payment.getStatus());
            }
        });
    }
}
