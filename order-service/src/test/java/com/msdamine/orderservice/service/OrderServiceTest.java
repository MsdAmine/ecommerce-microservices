
package com.msdamine.orderservice.service;

import com.msdamine.orderservice.client.ProductClient;
import com.msdamine.orderservice.dto.CreateOrderRequest;
import com.msdamine.orderservice.dto.OrderItemRequest;
import com.msdamine.orderservice.dto.OrderResponse;
import com.msdamine.orderservice.dto.ProductResponse;
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
import com.msdamine.orderservice.exception.InsufficientStockException;
import com.msdamine.orderservice.exception.ProductNotFoundException;
import feign.FeignException;
import feign.Request;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.msdamine.orderservice.exception.ProductServiceUnavailableException;
import feign.RetryableException;

import com.msdamine.orderservice.exception.DuplicateProductException;

class OrderServiceTest {

    private final OrderRepository orderRepository =
            mock(OrderRepository.class);

    private final OrderMapper orderMapper = new OrderMapper();

    private final ProductClient productClient =
            mock(ProductClient.class);

    private final OrderService orderService =
            new OrderService(orderRepository, orderMapper, productClient);

    @Test
    void shouldCalculateTotalUsingProductServicePrices() {

        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(
                15L,
                List.of(
                        new OrderItemRequest(101L, 2),
                        new OrderItemRequest(205L, 1)
                )
        );

        when(productClient.getProductById(101L))
                .thenReturn(new ProductResponse(
                        101L,
                        "Product A",
                        new BigDecimal("199.99"),
                        10
                ));

        when(productClient.getProductById(205L))
                .thenReturn(new ProductResponse(
                        205L,
                        "Product B",
                        new BigDecimal("49.99"),
                        5
                ));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderResponse response = orderService.createOrder(request);

        // Assert
        assertEquals(
                0,
                new BigDecimal("449.97")
                        .compareTo(response.totalAmount())
        );

        assertEquals(2, response.items().size());

        assertEquals(
                0,
                new BigDecimal("199.99")
                        .compareTo(response.items().get(0).unitPrice())
        );

        assertEquals(
                0,
                new BigDecimal("49.99")
                        .compareTo(response.items().get(1).unitPrice())
        );

        verify(productClient).getProductById(101L);
        verify(productClient).getProductById(205L);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void shouldCreateOrderWithCorrectTotalAndItems() {

        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(
                15L,
                List.of(
                        new OrderItemRequest(101L, 2),
                        new OrderItemRequest(205L, 1)
                )
        );

        when(productClient.getProductById(101L))
                .thenReturn(new ProductResponse(
                        101L,
                        "Product A",
                        new BigDecimal("199.99"),
                        10
                ));

        when(productClient.getProductById(205L))
                .thenReturn(new ProductResponse(
                        205L,
                        "Product B",
                        new BigDecimal("49.99"),
                        5
                ));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderResponse result = orderService.createOrder(request);

        // Capture the saved entity
        ArgumentCaptor<Order> captor =
                ArgumentCaptor.forClass(Order.class);

        verify(orderRepository).save(captor.capture());

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

        assertEquals(2, result.items().size());

        assertEquals(101L, result.items().get(0).productId());
        assertEquals(2, result.items().get(0).quantity());

        assertEquals(205L, result.items().get(1).productId());
        assertEquals(1, result.items().get(1).quantity());

        // Verify entity persistence data
        assertEquals(15L, savedOrder.getCustomerId());
        assertEquals(OrderStatus.PENDING, savedOrder.getStatus());

        assertEquals(
                0,
                new BigDecimal("449.97")
                        .compareTo(savedOrder.getTotalAmount())
        );

        assertEquals(2, savedOrder.getItems().size());

        // Verify bidirectional JPA relationship
        assertTrue(
                savedOrder.getItems().stream()
                        .allMatch(item -> item.getOrder() == savedOrder)
        );

        // Verify the stored prices
        assertEquals(
                0,
                new BigDecimal("199.99").compareTo(
                        savedOrder.getItems().get(0).getUnitPrice()
                )
        );

        assertEquals(
                0,
                new BigDecimal("49.99").compareTo(
                        savedOrder.getItems().get(1).getUnitPrice()
                )
        );

        verify(productClient).getProductById(101L);
        verify(productClient).getProductById(205L);
    }

    @Test
    void shouldRetrieveOrderById() {

        Long orderId = 1L;

        Order order = new Order(
                15L,
                new BigDecimal("449.97")
        );

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        OrderResponse response =
                orderService.getOrderById(orderId);

        assertEquals(15L, response.customerId());
        assertEquals(OrderStatus.PENDING, response.status());

        assertEquals(
                0,
                new BigDecimal("449.97")
                        .compareTo(response.totalAmount())
        );

        verify(orderRepository).findById(orderId);
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFound() {

        Long orderId = 999L;

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.empty());

        OrderNotFoundException exception = assertThrows(
                OrderNotFoundException.class,
                () -> orderService.getOrderById(orderId)
        );

        assertEquals(
                "Order not found with id: 999",
                exception.getMessage()
        );

        verify(orderRepository).findById(orderId);
    }

    @ParameterizedTest
    @EnumSource(
            value = OrderStatus.class,
            names = {"CONFIRMED", "CANCELLED"}
    )
    void shouldUpdatePendingOrderToValidStatus(OrderStatus newStatus) {

        Long orderId = 1L;

        Order order = new Order(
                15L,
                new BigDecimal("100.00")
        );

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        OrderResponse response =
                orderService.updateOrderStatus(orderId, newStatus);

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

        Long orderId = 1L;

        Order order = new Order(
                15L,
                new BigDecimal("100.00")
        );

        order.setStatus(currentStatus);

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        assertThrows(
                InvalidOrderStatusException.class,
                () -> orderService.updateOrderStatus(orderId, newStatus)
        );

        assertEquals(currentStatus, order.getStatus());
    }

    @Test
    void shouldRejectStatusUpdateForMissingOrder() {

        Long orderId = 999L;

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.empty());

        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.updateOrderStatus(
                        orderId,
                        OrderStatus.CONFIRMED
                )
        );

        verify(orderRepository).findById(orderId);
    }
    @Test
    void shouldRejectOrderWhenStockIsInsufficient() {

        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(
                15L,
                List.of(
                        new OrderItemRequest(2L, 15)
                )
        );

        when(productClient.getProductById(2L))
                .thenReturn(new ProductResponse(
                        2L,
                        "Test Mouse",
                        new BigDecimal("29.99"),
                        10
                ));

        // Act & Assert
        InsufficientStockException exception = assertThrows(
                InsufficientStockException.class,
                () -> orderService.createOrder(request)
        );

        assertEquals(
                "Insufficient stock for product 2: requested 15, available 10",
                exception.getMessage()
        );

        verify(productClient).getProductById(2L);

        verify(orderRepository, never()).save(any(Order.class));
    }
    @Test
    void shouldRejectOrderWhenProductDoesNotExist() {

        // Arrange
        Long productId = 999L;

        CreateOrderRequest request = new CreateOrderRequest(
                15L,
                List.of(new OrderItemRequest(productId, 2))
        );

        Request feignRequest = Request.create(
                Request.HttpMethod.GET,
                "http://product-service/api/products/999",
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                null
        );

        FeignException.NotFound feignException =
                new FeignException.NotFound(
                        "Product not found",
                        feignRequest,
                        null,
                        Map.of()
                );

        when(productClient.getProductById(productId))
                .thenThrow(feignException);

        // Act & Assert
        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> orderService.createOrder(request)
        );

        assertEquals(
                "Product not found with id: 999",
                exception.getMessage()
        );

        verify(productClient).getProductById(productId);

        // The order must not be persisted
        verify(orderRepository, never()).save(any(Order.class));
    }
    @Test
    void shouldRejectOrderWhenProductServiceIsUnavailable() {

        // Arrange
        Long productId = 2L;

        CreateOrderRequest request = new CreateOrderRequest(
                15L,
                List.of(new OrderItemRequest(productId, 2))
        );

        Request feignRequest = Request.create(
                Request.HttpMethod.GET,
                "http://product-service/api/products/2",
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                null
        );

        RetryableException feignException = new RetryableException(
                -1,
                "Connection refused",
                Request.HttpMethod.GET,
                new java.net.ConnectException("Connection refused"),
                (Long) null,
                feignRequest
        );

        when(productClient.getProductById(productId))
                .thenThrow(feignException);

        // Act & Assert
        ProductServiceUnavailableException exception = assertThrows(
                ProductServiceUnavailableException.class,
                () -> orderService.createOrder(request)
        );

        assertEquals(
                "Product Service is temporarily unavailable",
                exception.getMessage()
        );

        assertSame(feignException, exception.getCause());

        verify(productClient).getProductById(productId);

        verify(orderRepository, never())
                .save(any(Order.class));
    }
    @Test
    void shouldRejectOrderWithDuplicateProducts() {

        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(
                15L,
                List.of(
                        new OrderItemRequest(2L, 7),
                        new OrderItemRequest(2L, 6)
                )
        );

        // Act & Assert
        DuplicateProductException exception = assertThrows(
                DuplicateProductException.class,
                () -> orderService.createOrder(request)
        );

        assertEquals(
                "Duplicate product in order: 2",
                exception.getMessage()
        );

        // Duplicate validation must happen before Feign calls
        verifyNoInteractions(productClient);

        // No order should be persisted
        verify(orderRepository, never())
                .save(any(Order.class));
    }
}
