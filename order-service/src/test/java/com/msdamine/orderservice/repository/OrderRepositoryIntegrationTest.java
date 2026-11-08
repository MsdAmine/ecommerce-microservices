package com.msdamine.orderservice.repository;

import com.msdamine.orderservice.entity.Order;
import com.msdamine.orderservice.enums.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import com.msdamine.orderservice.service.OrderService;
import java.math.BigDecimal;

import com.msdamine.orderservice.entity.OrderItem;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OrderRepositoryIntegrationTest {
    @Autowired
    private OrderService orderService;

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

    @Test
    void shouldSaveOrderWithItems() {

        // Arrange
        Order order = new Order(15L, new BigDecimal("449.97"));

        OrderItem item1 = new OrderItem(
                101L, 2, new BigDecimal("199.99")
        );

        OrderItem item2 = new OrderItem(
                205L, 1, new BigDecimal("49.99")
        );

        order.addItem(item1);
        order.addItem(item2);

        // Act
        Order savedOrder = orderRepository.saveAndFlush(order);
        Long orderId = savedOrder.getId();

        entityManager.clear();

        Order foundOrder = orderRepository.findById(orderId)
                .orElseThrow();

        // Assert
        assertEquals(2, foundOrder.getItems().size());

        assertTrue(foundOrder.getItems().stream()
                .allMatch(item -> item.getOrder().getId().equals(orderId)));
    }
    @Test
    void shouldPersistOrderStatusChangeThroughDirtyChecking() {

        // Arrange: create an order in the database
        Order order = new Order(
                15L,
                new BigDecimal("199.99")
        );

        Order savedOrder = orderRepository.saveAndFlush(order);
        Long orderId = savedOrder.getId();

        // Clear the persistence context
        entityManager.clear();

        // Act: update through the service
        orderService.updateOrderStatus(
                orderId,
                OrderStatus.CONFIRMED
        );

        // Force pending changes to the database
        entityManager.flush();
        entityManager.clear();

        // Assert: reload the order from the database
        Order updatedOrder = orderRepository.findById(orderId)
                .orElseThrow();

        assertEquals(
                OrderStatus.CONFIRMED,
                updatedOrder.getStatus()
        );
    }

}