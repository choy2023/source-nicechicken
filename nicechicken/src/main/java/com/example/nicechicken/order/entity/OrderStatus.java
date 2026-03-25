package com.example.nicechicken.order.entity;

import java.util.Set;

/**
 * Order status with state machine transitions.
 *
 * Valid transitions:
 *   PENDING  → PAID, CANCELLED
 *   PAID     → COOKING, CANCELLED
 *   COOKING  → READY
 *   READY    → PICKED_UP
 *   PICKED_UP → (terminal)
 *   CANCELLED → (terminal)
 */
public enum OrderStatus {
    PENDING(Set.of("PAID", "CANCELLED")),
    PAID(Set.of("COOKING", "CANCELLED")),
    COOKING(Set.of("READY")),
    READY(Set.of("PICKED_UP")),
    PICKED_UP(Set.of()),    // terminal state
    CANCELLED(Set.of());    // terminal state

    private final Set<String> allowedTransitions;

    OrderStatus(Set<String> allowedTransitions) {
        this.allowedTransitions = allowedTransitions;
    }

    public boolean canTransitionTo(OrderStatus newStatus) {
        return allowedTransitions.contains(newStatus.name());
    }
}
