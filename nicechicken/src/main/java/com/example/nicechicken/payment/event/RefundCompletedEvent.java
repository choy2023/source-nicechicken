package com.example.nicechicken.payment.event;

import java.util.UUID;

/**
 * Event published when a refund is successfully completed
 */
public record RefundCompletedEvent(UUID orderId) {
}
