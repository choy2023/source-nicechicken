package com.example.nicechicken.notification.service;

import com.example.nicechicken.order.entity.OrderStatus;
import com.example.nicechicken.order.event.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final EmailService emailService;

    @EventListener
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("📧 Notification Module: Order status change event received. Status: {} -> {}", event.oldStatus(), event.newStatus());
        
        String subject = "[Nice Chicken] Update on your order status.";
        String message = getStatusMessage(event.newStatus());
        
        String content = "<h1>Order Status Notification</h1>" +
                "<p>Order Number: " + event.orderId() + "</p>" +
                "<p><b>Status: " + event.newStatus() + "</b></p>" +
                "<p>" + message + "</p>";
        
        emailService.sendHtmlEmail(event.customerEmail(), subject, content);
    }

    private String getStatusMessage(OrderStatus status) {
        return switch (status) {
            case PAID -> "Payment completed and order confirmed.";
            case COOKING -> "We are frying your chicken to perfection! 🍗";
            case READY -> "Your chicken is ready! Please pick it up or wait for delivery. 🛵";
            case PICKED_UP -> "Enjoy your meal! We hope to serve you again soon. 😊";
            case CANCELLED -> "Order cancelled. Refund process is underway.";
            case PENDING -> "Order received and awaiting payment.";
        };
    }
}
