package com.msdamine.orderservice.mapper;

import com.msdamine.orderservice.dto.OrderItemResponse;
import com.msdamine.orderservice.dto.OrderResponse;
import com.msdamine.orderservice.entity.Order;
import com.msdamine.orderservice.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {
    public OrderItemResponse toItemResponse(OrderItem item) {

        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getQuantity(),
                item.getUnitPrice()
        );
    }
    public OrderResponse toOrderResponse(Order order) {

        List<OrderItemResponse> itemResponses = order.getItems()
                .stream()
                .map(this::toItemResponse)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                itemResponses
        );
    }

}