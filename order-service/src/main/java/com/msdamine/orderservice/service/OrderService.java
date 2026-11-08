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
import java.util.List;import com.msdamine.orderservice.enums.OrderStatus;
import com.msdamine.orderservice.exception.InvalidOrderStatusException;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    public OrderService(
            OrderRepository orderRepository,
            OrderMapper orderMapper
    ) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
    }
    public BigDecimal calculateTotal(List<OrderItemRequest> items) {

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest item : items) {

            BigDecimal subtotal = item.unitPrice()
                    .multiply(BigDecimal.valueOf(item.quantity()));

            total = total.add(subtotal);
        }

        return total;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {

        BigDecimal total = calculateTotal(request.items());

        Order order = new Order(request.customerId(), total);

        for (OrderItemRequest itemRequest : request.items()) {

            OrderItem item = new OrderItem(
                    itemRequest.productId(),
                    itemRequest.quantity(),
                    itemRequest.unitPrice()
            );

            order.addItem(item);
        }

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