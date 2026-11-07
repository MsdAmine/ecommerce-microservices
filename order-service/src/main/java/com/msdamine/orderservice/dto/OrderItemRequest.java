package com.msdamine.orderservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record OrderItemRequest(

        @NotNull
        @Positive
        Long productId,

        @NotNull
        @Positive
        Integer quantity,

        @NotNull
        @DecimalMin("0.0")
        BigDecimal unitPrice

) {
}