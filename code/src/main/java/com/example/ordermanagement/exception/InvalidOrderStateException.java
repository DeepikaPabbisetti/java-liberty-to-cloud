package com.example.ordermanagement.exception;

import com.example.ordermanagement.enums.OrderStatus;

public class InvalidOrderStateException extends RuntimeException {

    public InvalidOrderStateException(OrderStatus current, OrderStatus target) {
        super(String.format(
                "Cannot transition order from '%s' to '%s'",
                current, target));
    }

    public InvalidOrderStateException(String message) {
        super(message);
    }
}
