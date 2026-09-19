package com.unicorn.gameshop.order.model;

public enum PaymentStatus {
    CREATED,
    PAYPAL_ORDER_CREATED,
    CUSTOMER_APPROVED,
    CAPTURE_PENDING,
    CAPTURED,
    CAPTURE_FAILED,
    PARTIALLY_REFUNDED,
    REFUNDED,
    DISPUTED
}
