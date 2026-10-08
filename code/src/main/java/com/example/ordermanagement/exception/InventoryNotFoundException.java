package com.example.ordermanagement.exception;

public class InventoryNotFoundException extends ResourceNotFoundException {

    public InventoryNotFoundException(Long productId) {
        super("Inventory record not found for product id: " + productId);
    }
}
