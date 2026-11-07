package com.msdamine.orderservice.service;

import com.msdamine.orderservice.dto.OrderItemRequest;
import org.springframework.stereotype.Service;
import com.msdamine.orderservice.repository.OrderRepository;
import com.msdamine.orderservice.dto.CreateOrderRequest;
import com.msdamine.orderservice.entity.Order;
import com.msdamine.orderservice.entity.OrderItem;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
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
    public Order createOrder(CreateOrderRequest request) {

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

        return orderRepository.save(order);
    }
}