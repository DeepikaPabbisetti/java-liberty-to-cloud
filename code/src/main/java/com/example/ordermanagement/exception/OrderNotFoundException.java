package com.example.ordermanagement.exception;

public class OrderNotFoundException extends ResourceNotFoundException {

    public OrderNotFoundException(Long id) {
        super("Order not found with id: " + id);
    }

    public OrderNotFoundException(String orderNumber) {
        super("Order not found with order number: " + orderNumber);
    }
}
