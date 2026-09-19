package com.unicorn.gameshop.refund.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record RefundRequest(
        @Positive long amountMinor,
        @NotBlank String currency,
        @NotBlank String reason
) {
}
