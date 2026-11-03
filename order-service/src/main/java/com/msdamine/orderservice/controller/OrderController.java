package com.msdamine.orderservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    @GetMapping("/api/orders/health")
    public String healthCheck() {
        return "Order Service is running!";
    }
}