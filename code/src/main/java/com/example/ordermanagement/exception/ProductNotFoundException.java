package com.example.ordermanagement.exception;

public class ProductNotFoundException extends ResourceNotFoundException {

    public ProductNotFoundException(Long id) {
        super("Product not found with id: " + id);
    }

    public ProductNotFoundException(String productCode) {
        super("Product not found with product code: " + productCode);
    }
}
