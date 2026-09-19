package com.unicorn.gameshop.payment.dto;

public record CaptureResponse(
        String orderId,
        String paymentStatus,
        String fulfillmentStatus,
        String captureId
) {
}
