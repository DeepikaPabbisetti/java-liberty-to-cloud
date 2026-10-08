package com.example.ordermanagement.exception;

public class InsufficientInventoryException extends RuntimeException {

    public InsufficientInventoryException(String productCode, int requested, int available) {
        super(String.format(
                "Insufficient inventory for product '%s': requested %d, available %d",
                productCode, requested, available));
    }
}
