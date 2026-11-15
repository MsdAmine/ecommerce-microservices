package com.msdamine.orderservice.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CreateOrderRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory =
                Validation.buildDefaultValidatorFactory();

        validator = factory.getValidator();
    }

    @Test
    void shouldAcceptValidOrderRequest() {

        CreateOrderRequest request = new CreateOrderRequest(
                15L,
                List.of(new OrderItemRequest(101L, 2))
        );

        var violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldRejectInvalidOrderRequest() {

        CreateOrderRequest request = new CreateOrderRequest(
                null,
                List.of(new OrderItemRequest(-1L, 0))
        );

        var violations = validator.validate(request);

        assertEquals(3, violations.size());    }
}