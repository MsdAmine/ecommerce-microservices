package com.msdamine.orderservice.repository;

import com.msdamine.orderservice.entity.Order;
import com.msdamine.orderservice.enums.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
class OrderRepositoryIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldSaveAndRetrieveOrder() {

        // Arrange
        Order order = new Order(1L, new BigDecimal("299.99"));

        // Act
        Order savedOrder = orderRepository.saveAndFlush(order);

        Long orderId = savedOrder.getId();

        entityManager.clear();

        Order foundOrder = orderRepository.findById(orderId)
                .orElseThrow();

        // Assert
        assertNotNull(savedOrder.getId());
        assertEquals(1L, foundOrder.getCustomerId());
        assertEquals(OrderStatus.PENDING, foundOrder.getStatus());
        assertEquals(0, new BigDecimal("299.99").compareTo(foundOrder.getTotalAmount()));
        assertNotNull(foundOrder.getCreatedAt());
    }

}