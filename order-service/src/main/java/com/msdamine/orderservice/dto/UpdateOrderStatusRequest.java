package com.msdamine.orderservice.dto;

import com.msdamine.orderservice.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull OrderStatus status
) {
}