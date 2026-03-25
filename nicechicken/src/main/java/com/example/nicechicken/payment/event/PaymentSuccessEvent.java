package com.example.nicechicken.payment.event;

import java.util.UUID;

/**
 * Event published upon successful payment
 */
public record PaymentSuccessEvent(UUID orderId) {
}
