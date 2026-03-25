package com.example.nicechicken.order.event;

import com.example.nicechicken.order.entity.OrderStatus;
import java.util.UUID;

/**
 * Event published when the order status is changed
 */
public record OrderStatusChangedEvent(
    UUID orderId,
    OrderStatus oldStatus,
    OrderStatus newStatus,
    String customerEmail
) {
}
