package com.example.ordermanagement.enums;

import java.util.Set;

public enum OrderStatus {

    CREATED,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public Set<OrderStatus> allowedTransitions() {
        return switch (this) {
            case CREATED    -> Set.of(CONFIRMED, CANCELLED);
            case CONFIRMED  -> Set.of(PROCESSING, CANCELLED);
            case PROCESSING -> Set.of(SHIPPED, CANCELLED);
            case SHIPPED    -> Set.of(DELIVERED);
            case DELIVERED  -> Set.of();
            case CANCELLED  -> Set.of();
        };
    }

    public boolean canTransitionTo(OrderStatus next) {
        return allowedTransitions().contains(next);
    }

    public boolean isCancellable() {
        return canTransitionTo(CANCELLED);
    }
}
