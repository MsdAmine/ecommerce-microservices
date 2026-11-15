package com.msdamine.orderservice.exception;

public class ProductServiceUnavailableException extends RuntimeException {

    public ProductServiceUnavailableException(Throwable cause) {
        super("Product Service is temporarily unavailable", cause);
    }
}