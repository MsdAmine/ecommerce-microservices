package com.msdamine.orderservice.exception;

import com.msdamine.orderservice.enums.OrderStatus;

public class InvalidOrderStatusException extends RuntimeException {

    public InvalidOrderStatusException(
            OrderStatus currentStatus,
            OrderStatus newStatus
    ) {
        super("Cannot change order status from "
                + currentStatus + " to " + newStatus);
    }
}