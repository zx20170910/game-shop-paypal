package com.unicorn.gameshop.catalog.dto;

import jakarta.validation.constraints.NotBlank;

public record GameRequest(
        @NotBlank String code,
        @NotBlank String name,
        String publisher,
        String supportedPlatforms,
        String deliveryType,
        @NotBlank String status
) {
}
