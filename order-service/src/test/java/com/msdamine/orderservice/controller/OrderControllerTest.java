package com.msdamine.orderservice.controller;

import com.msdamine.orderservice.dto.OrderResponse;
import com.msdamine.orderservice.enums.OrderStatus;
import com.msdamine.orderservice.mapper.OrderMapper;
import com.msdamine.orderservice.service.OrderService;
import com.msdamine.orderservice.dto.CreateOrderRequest;
import com.msdamine.orderservice.dto.OrderItemRequest;
import com.msdamine.orderservice.entity.Order;
import com.msdamine.orderservice.entity.OrderItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.mockito.Mockito.never;
import com.msdamine.orderservice.dto.UpdateOrderStatusRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.msdamine.orderservice.exception.OrderNotFoundException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import com.msdamine.orderservice.exception.InvalidOrderStatusException;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private OrderMapper orderMapper;

    @Test
    void shouldReturnOrderWhenOrderExists() throws Exception {

        // Arrange
        Long orderId = 1L;

        OrderResponse response = new OrderResponse(
                orderId,
                15L,
                OrderStatus.PENDING,
                new BigDecimal("449.97"),
                LocalDateTime.of(2026, 11, 8, 16, 0),
                List.of()
        );

        when(orderService.getOrderById(orderId))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        "application/json"
                ))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.customerId").value(15))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(449.97))
                .andExpect(jsonPath("$.items").isArray());
    }
    @Test
    void shouldReturn404WhenOrderDoesNotExist() throws Exception {

        // Arrange
        Long orderId = 999L;

        when(orderService.getOrderById(orderId))
                .thenThrow(new OrderNotFoundException(orderId));

        // Act & Assert
        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Order not found with id: 999"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService).getOrderById(orderId);
    }
    @Test
    void shouldCreateOrderAndReturn201() throws Exception {

        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(
                15L,
                List.of(
                        new OrderItemRequest(
                                101L,
                                2,
                                new BigDecimal("199.99")
                        )
                )
        );

        OrderResponse response = new OrderResponse(
                1L,
                15L,
                OrderStatus.PENDING,
                new BigDecimal("399.98"),
                LocalDateTime.of(2026, 11, 8, 16, 0),
                List.of()
        );

        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location", "/api/orders/1"
                ))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.customerId").value(15))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(399.98));

        verify(orderService).createOrder(any(CreateOrderRequest.class));
    }
    @Test
    void shouldReturn400WhenCreateOrderRequestIsInvalid() throws Exception {

        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(
                -5L,
                List.of()
        );

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never())
                .createOrder(any(CreateOrderRequest.class));
    }
    @Test
    void shouldUpdateOrderStatusAndReturn200() throws Exception {

        // Arrange
        Long orderId = 1L;

        UpdateOrderStatusRequest request =
                new UpdateOrderStatusRequest(OrderStatus.CONFIRMED);

        OrderResponse response = new OrderResponse(
                orderId,
                15L,
                OrderStatus.CONFIRMED,
                new BigDecimal("399.98"),
                LocalDateTime.of(2026, 11, 8, 16, 0),
                List.of()
        );

        when(orderService.updateOrderStatus(
                orderId, OrderStatus.CONFIRMED
        )).thenReturn(response);

        // Act & Assert
        mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalAmount").value(399.98));

        verify(orderService).updateOrderStatus(
                orderId, OrderStatus.CONFIRMED
        );
    }
    @Test
    void shouldReturn409WhenStatusTransitionIsInvalid() throws Exception {

        // Arrange
        Long orderId = 1L;

        UpdateOrderStatusRequest request =
                new UpdateOrderStatusRequest(OrderStatus.CANCELLED);

        when(orderService.updateOrderStatus(
                orderId, OrderStatus.CANCELLED
        )).thenThrow(
                new InvalidOrderStatusException(
                        OrderStatus.CONFIRMED,
                        OrderStatus.CANCELLED
                )
        );

        // Act & Assert
        mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(
                        "Cannot change order status from CONFIRMED to CANCELLED"
                ))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService).updateOrderStatus(
                orderId, OrderStatus.CANCELLED
        );
    }
    @Test
    void shouldReturn400WhenOrderStatusIsMissing() throws Exception {

        // Arrange
        Long orderId = 1L;

        UpdateOrderStatusRequest request =
                new UpdateOrderStatusRequest(null);

        // Act & Assert
        mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(orderService, never())
                .updateOrderStatus(
                        any(Long.class),
                        any(OrderStatus.class)
                );
    }
    @Test
    void shouldReturn400WhenOrderItemIsInvalid() throws Exception {

        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(
                15L,
                List.of(
                        new OrderItemRequest(
                                101L,
                                -3,
                                new BigDecimal("199.99")
                        )
                )
        );

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("quantity")))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(orderService);
    }
    @Test
    void shouldReturn400WhenOrderStatusIsInvalid() throws Exception {

        String invalidJson = """
            {
                "status": "SHIPPED"
            }
            """;

        mockMvc.perform(patch("/api/orders/{id}/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(
                        "Malformed JSON or invalid request field value"
                ));

        verifyNoInteractions(orderService);
    }
    @Test
    void shouldReturn400WhenJsonIsMalformed() throws Exception {

        String malformedJson = """
            {
                "status": "CONFIRMED"
            """;

        mockMvc.perform(patch("/api/orders/{id}/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(
                        "Malformed JSON or invalid request field value"
                ));

        verifyNoInteractions(orderService);
    }
}