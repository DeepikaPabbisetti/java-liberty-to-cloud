package com.example.ordermanagement.exception;

public class CustomerNotFoundException extends ResourceNotFoundException {

    public CustomerNotFoundException(Long id) {
        super("Customer not found with id: " + id);
    }

    public CustomerNotFoundException(String customerNumber) {
        super("Customer not found with customer number: " + customerNumber);
    }
}
