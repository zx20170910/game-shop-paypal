package com.unicorn.gameshop.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateCheckoutSessionRequest(
        @NotBlank String productId,
        @Min(1) @Max(99) int quantity,
        @NotBlank String gameId,
        @NotBlank String playerUid,
        @NotBlank String country,
        @NotBlank String currency
) {
}
