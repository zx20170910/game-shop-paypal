package com.unicorn.gameshop.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record ProductRequest(
        @NotBlank String gameId,
        @NotBlank String sku,
        @NotBlank String name,
        @PositiveOrZero long amountMinor,
        @NotBlank String currency,
        @NotBlank String platform,
        @NotBlank String serverRegion,
        @NotBlank String deliveryType,
        @NotBlank String status
) {
}
