package com.msdamine.orderservice.controller;

import com.msdamine.orderservice.dto.OrderResponse;
import com.msdamine.orderservice.service.OrderService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.msdamine.orderservice.dto.CreateOrderRequest;
import com.msdamine.orderservice.entity.Order;
import com.msdamine.orderservice.mapper.OrderMapper;
import com.msdamine.orderservice.dto.UpdateOrderStatusRequest;
import jakarta.validation.Valid;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.URI;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    public OrderController(
            OrderService orderService,
            OrderMapper orderMapper
    ) {
        this.orderService = orderService;
        this.orderMapper = orderMapper;
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable Long id
    ) {
        OrderResponse response = orderService.getOrderById(id);

        return ResponseEntity.ok(response);
    }
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {

        OrderResponse response = orderService.createOrder(request);

        URI location = URI.create("/api/orders/" + response.id());

        return ResponseEntity
                .created(location)
                .body(response);
    }
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {

        OrderResponse response = orderService.updateOrderStatus(
                id,
                request.status()
        );

        return ResponseEntity.ok(response);
    }
}