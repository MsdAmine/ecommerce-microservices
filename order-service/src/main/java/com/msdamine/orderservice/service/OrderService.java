package com.msdamine.orderservice.service;

import com.msdamine.orderservice.dto.OrderItemRequest;
import org.springframework.stereotype.Service;
import com.msdamine.orderservice.repository.OrderRepository;
import com.msdamine.orderservice.dto.CreateOrderRequest;
import com.msdamine.orderservice.entity.Order;
import com.msdamine.orderservice.entity.OrderItem;
import org.springframework.transaction.annotation.Transactional;
import com.msdamine.orderservice.mapper.OrderMapper;
import com.msdamine.orderservice.dto.OrderResponse;
import com.msdamine.orderservice.exception.OrderNotFoundException;

import java.math.BigDecimal;
import java.util.List;
import com.msdamine.orderservice.enums.OrderStatus;
import com.msdamine.orderservice.exception.InvalidOrderStatusException;
import com.msdamine.orderservice.exception.InsufficientStockException;

import com.msdamine.orderservice.client.ProductClient;
import com.msdamine.orderservice.dto.ProductResponse;
import feign.FeignException;
import com.msdamine.orderservice.exception.ProductNotFoundException;
import com.msdamine.orderservice.exception.ProductServiceUnavailableException;
import feign.RetryableException;
import com.msdamine.orderservice.exception.DuplicateProductException;
import java.util.HashSet;
import java.util.Set;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ProductClient productClient;


    public OrderService(
            OrderRepository orderRepository,
            OrderMapper orderMapper,
            ProductClient productClient
    ) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.productClient = productClient;
    }


    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {

        BigDecimal total = BigDecimal.ZERO;
        Set<Long> productIds = new HashSet<>();

        for (OrderItemRequest item : request.items()) {
            if (!productIds.add(item.productId())) {
                throw new DuplicateProductException(item.productId());
            }
        }

        Order order = new Order(
                request.customerId(),
                BigDecimal.ZERO
        );

        for (OrderItemRequest itemRequest : request.items()) {

            // Retrieve product information from Product Service
            ProductResponse product;

            try {
                product = productClient.getProductById(
                        itemRequest.productId()
                );

            } catch (FeignException.NotFound exception) {

                throw new ProductNotFoundException(
                        itemRequest.productId()
                );

            } catch (RetryableException exception) {

                throw new ProductServiceUnavailableException(exception);

            } catch (FeignException exception) {

                throw new ProductServiceUnavailableException(exception);
            }
            if (itemRequest.quantity() > product.stockQuantity()) {
                throw new InsufficientStockException(
                        itemRequest.productId(),
                        itemRequest.quantity(),
                        product.stockQuantity()
                );
            }

            // Use the trusted product price
            BigDecimal unitPrice = product.price();

            // Calculate the item subtotal
            BigDecimal subtotal = unitPrice.multiply(
                    BigDecimal.valueOf(itemRequest.quantity())
            );

            total = total.add(subtotal);

            // Create the order item using the trusted price
            OrderItem item = new OrderItem(
                    itemRequest.productId(),
                    itemRequest.quantity(),
                    unitPrice
            );

            order.addItem(item);
        }

        // Assign the final total
        order.setTotalAmount(total);

        // Persist the order and its items
        Order savedOrder = orderRepository.save(order);

        return orderMapper.toOrderResponse(savedOrder);
    }
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        return orderMapper.toOrderResponse(order);
    }
    @Transactional
    public OrderResponse updateOrderStatus(
            Long id,
            OrderStatus newStatus
    ) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        OrderStatus currentStatus = order.getStatus();

        if (newStatus == null
                || currentStatus != OrderStatus.PENDING
                || newStatus == OrderStatus.PENDING) {

            throw new InvalidOrderStatusException(
                    currentStatus, newStatus
            );
        }

        order.setStatus(newStatus);

        return orderMapper.toOrderResponse(order);
    }
}