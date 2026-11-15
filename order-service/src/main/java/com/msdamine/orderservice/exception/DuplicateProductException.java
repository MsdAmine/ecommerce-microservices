package com.msdamine.orderservice.exception;

public class DuplicateProductException extends RuntimeException {

    public DuplicateProductException(Long productId) {
        super("Duplicate product in order: " + productId);
    }
}