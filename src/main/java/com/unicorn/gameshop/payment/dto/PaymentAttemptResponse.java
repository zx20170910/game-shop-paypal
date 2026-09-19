package com.unicorn.gameshop.payment.dto;

public record PaymentAttemptResponse(
        String paymentAttemptId,
        String provider,
        String paypalOrderId,
        String status
) {
}
