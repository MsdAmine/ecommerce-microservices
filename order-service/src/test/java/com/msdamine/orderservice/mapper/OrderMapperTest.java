package com.msdamine.orderservice.mapper;

import com.msdamine.orderservice.dto.OrderResponse;
import com.msdamine.orderservice.entity.Order;
import com.msdamine.orderservice.entity.OrderItem;
import com.msdamine.orderservice.enums.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderMapperTest {

    private final OrderMapper orderMapper = new OrderMapper();

    @Test
    void shouldMapOrderEntityToResponse() {

        // Arrange
        Order order = new Order(
                15L,
                new BigDecimal("449.97")
        );

        OrderItem item1 = new OrderItem(
                101L, 2, new BigDecimal("199.99")
        );

        OrderItem item2 = new OrderItem(
                205L, 1, new BigDecimal("49.99")
        );

        order.addItem(item1);
        order.addItem(item2);

        // Act
        OrderResponse response = orderMapper.toOrderResponse(order);

        // Assert
        assertEquals(15L, response.customerId());
        assertEquals(OrderStatus.PENDING, response.status());

        assertEquals(
                0,
                new BigDecimal("449.97")
                        .compareTo(response.totalAmount())
        );

        assertEquals(2, response.items().size());

        assertEquals(101L, response.items().get(0).productId());
        assertEquals(2, response.items().get(0).quantity());

        assertEquals(205L, response.items().get(1).productId());
        assertEquals(1, response.items().get(1).quantity());
    }
}