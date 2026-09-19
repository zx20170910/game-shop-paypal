package com.unicorn.gameshop.risk.dto;

import jakarta.validation.constraints.NotBlank;

public record RiskReviewRequest(@NotBlank String decision, String reason) {
}
