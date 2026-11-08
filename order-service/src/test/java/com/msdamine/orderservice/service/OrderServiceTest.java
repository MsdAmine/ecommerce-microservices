
package com.msdamine.orderservice.service;

import com.msdamine.orderservice.dto.CreateOrderRequest;
import com.msdamine.orderservice.dto.OrderItemRequest;
import com.msdamine.orderservice.dto.OrderResponse;
import com.msdamine.orderservice.entity.Order;
import com.msdamine.orderservice.enums.OrderStatus;
import com.msdamine.orderservice.exception.InvalidOrderStatusException;
import com.msdamine.orderservice.exception.OrderNotFoundException;
import com.msdamine.orderservice.mapper.OrderMapper;
import com.msdamine.orderservice.repository.OrderRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    private final OrderRepository orderRepository =
            mock(OrderRepository.class);

    // Real mapper: no Mockito mocking needed
    private final OrderMapper orderMapper = new OrderMapper();

    private final OrderService orderService =
            new OrderService(orderRepository, orderMapper);

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
        assertEquals(
                0,
                new BigDecimal("449.97").compareTo(total)
        );
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

        BigDecimal total =
                orderService.calculateTotal(List.of());

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

        // Mock only the repository
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderResponse result =
                orderService.createOrder(request);

        // Assert: verify repository interaction
        ArgumentCaptor<Order> captor =
                ArgumentCaptor.forClass(Order.class);

        verify(orderRepository, times(1))
                .save(captor.capture());

        Order savedOrder = captor.getValue();

        // Verify response DTO
        assertNotNull(result);
        assertEquals(15L, result.customerId());
        assertEquals(OrderStatus.PENDING, result.status());

        assertEquals(
                0,
                new BigDecimal("449.97")
                        .compareTo(result.totalAmount())
        );

        // Verify response items
        assertEquals(2, result.items().size());

        assertEquals(101L, result.items().get(0).productId());
        assertEquals(2, result.items().get(0).quantity());

        assertEquals(205L, result.items().get(1).productId());
        assertEquals(1, result.items().get(1).quantity());

        // Verify saved entity
        assertEquals(15L, savedOrder.getCustomerId());
        assertEquals(OrderStatus.PENDING, savedOrder.getStatus());

        assertEquals(
                0,
                new BigDecimal("449.97")
                        .compareTo(savedOrder.getTotalAmount())
        );

        assertEquals(2, savedOrder.getItems().size());

        // Verify bidirectional relationship
        assertTrue(
                savedOrder.getItems().stream()
                        .allMatch(item -> item.getOrder() == savedOrder)
        );
    }

    @Test
    void shouldRetrieveOrderById() {

        // Arrange
        Long orderId = 1L;

        Order order = new Order(
                15L,
                new BigDecimal("449.97")
        );

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        // Act
        OrderResponse response =
                orderService.getOrderById(orderId);

        // Assert
        assertEquals(15L, response.customerId());
        assertEquals(OrderStatus.PENDING, response.status());

        assertEquals(
                0,
                new BigDecimal("449.97")
                        .compareTo(response.totalAmount())
        );

        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFound() {

        // Arrange
        Long orderId = 999L;

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.empty());

        // Act & Assert
        OrderNotFoundException exception = assertThrows(
                OrderNotFoundException.class,
                () -> orderService.getOrderById(orderId)
        );

        assertEquals(
                "Order not found with id: 999",
                exception.getMessage()
        );

        verify(orderRepository, times(1)).findById(orderId);
    }

    @ParameterizedTest
    @EnumSource(
            value = OrderStatus.class,
            names = {"CONFIRMED", "CANCELLED"}
    )
    void shouldUpdatePendingOrderToValidStatus(OrderStatus newStatus) {

        // Arrange
        Long orderId = 1L;

        Order order = new Order(
                15L,
                new BigDecimal("100.00")
        );

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        // Act
        OrderResponse response =
                orderService.updateOrderStatus(orderId, newStatus);

        // Assert
        assertEquals(newStatus, response.status());
        assertEquals(newStatus, order.getStatus());

        verify(orderRepository).findById(orderId);
    }

    @ParameterizedTest
    @CsvSource({
            "CONFIRMED, CANCELLED",
            "CANCELLED, CONFIRMED",
            "PENDING, PENDING"
    })
    void shouldRejectInvalidStatusTransitions(
            OrderStatus currentStatus,
            OrderStatus newStatus
    ) {

        // Arrange
        Long orderId = 1L;

        Order order = new Order(
                15L,
                new BigDecimal("100.00")
        );

        order.setStatus(currentStatus);

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(
                InvalidOrderStatusException.class,
                () -> orderService.updateOrderStatus(orderId, newStatus)
        );

        assertEquals(currentStatus, order.getStatus());
    }

    @Test
    void shouldRejectStatusUpdateForMissingOrder() {

        // Arrange
        Long orderId = 999L;

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.updateOrderStatus(
                        orderId,
                        OrderStatus.CONFIRMED
                )
        );

        verify(orderRepository).findById(orderId);
    }
}
