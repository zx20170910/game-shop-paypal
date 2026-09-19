package com.unicorn.gameshop.order.dto;

import java.time.Instant;

public record CreateCheckoutSessionResponse(
        String sessionId,
        String orderId,
        String accessToken,
        long amountMinor,
        String currency,
        Instant expiresAt
) {
}
