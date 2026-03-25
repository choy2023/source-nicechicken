package com.example.nicechicken.order.event;

import java.util.List;
import java.util.UUID;

/**
 * Event published upon order cancellation
 */
public record OrderCancelledEvent(UUID orderId, List<CancelledItem> items) {
    public record CancelledItem(Long productId, int quantity) {}
}
