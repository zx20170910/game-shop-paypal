package com.unicorn.gameshop.refund.dto;

public record RefundResponse(
        String refundId,
        String providerRefundId,
        long amountMinor,
        String currency,
        String status
) {
}
