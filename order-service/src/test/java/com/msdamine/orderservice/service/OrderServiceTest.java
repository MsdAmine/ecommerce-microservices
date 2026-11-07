package com.msdamine.orderservice.service;

import com.msdamine.orderservice.dto.OrderItemRequest;
import com.msdamine.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;


import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import com.msdamine.orderservice.dto.CreateOrderRequest;
import com.msdamine.orderservice.entity.Order;
import com.msdamine.orderservice.enums.OrderStatus;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


class OrderServiceTest {

    private final OrderRepository orderRepository =
            mock(OrderRepository.class);

    private final OrderService orderService =
            new OrderService(orderRepository);
    @Test
    void shouldCalculateTotalForMultipleItems() {

        // Arrange
        List<OrderItemRequest> items = List.of(
                new OrderItemRequest(
                        101L, 2, new BigDecimal("199.99")
                ),
                new OrderItemRequest(
                        205L, 1, new BigDecimal("49.99")
                )
        );

        // Act
        BigDecimal total = orderService.calculateTotal(items);

        // Assert
        assertEquals(0, new BigDecimal("449.97").compareTo(total));
    }

    @Test
    void shouldCalculateTotalForFreeProduct() {

        List<OrderItemRequest> items = List.of(
                new OrderItemRequest(
                        101L, 2, BigDecimal.ZERO
                )
        );

        BigDecimal total = orderService.calculateTotal(items);

        assertEquals(0, BigDecimal.ZERO.compareTo(total));
    }

    @Test
    void shouldReturnZeroForEmptyItems() {

        BigDecimal total = orderService.calculateTotal(List.of());

        assertEquals(0, BigDecimal.ZERO.compareTo(total));
    }

    @Test
    void shouldCreateOrderWithCorrectTotalAndItems() {

        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(
                15L,
                List.of(
                        new OrderItemRequest(
                                101L, 2, new BigDecimal("199.99")
                        ),
                        new OrderItemRequest(
                                205L, 1, new BigDecimal("49.99")
                        )
                )
        );

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Order result = orderService.createOrder(request);

        // Assert
        ArgumentCaptor<Order> captor =
                ArgumentCaptor.forClass(Order.class);

        verify(orderRepository, times(1)).save(captor.capture());

        Order savedOrder = captor.getValue();

        assertSame(savedOrder, result);
        assertEquals(15L, savedOrder.getCustomerId());
        assertEquals(OrderStatus.PENDING, savedOrder.getStatus());

        assertEquals(
                0,
                new BigDecimal("449.97")
                        .compareTo(savedOrder.getTotalAmount())
        );

        assertEquals(2, savedOrder.getItems().size());

        assertTrue(savedOrder.getItems().stream()
                .allMatch(item -> item.getOrder() == savedOrder));
    }
}