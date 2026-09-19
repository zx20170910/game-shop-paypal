package com.unicorn.gameshop.catalog.dto;

import jakarta.validation.constraints.NotBlank;

public record ServerRequest(
        @NotBlank String gameId,
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String region,
        @NotBlank String status
) {
}
