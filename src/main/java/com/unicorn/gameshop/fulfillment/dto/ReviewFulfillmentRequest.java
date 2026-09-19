package com.unicorn.gameshop.fulfillment.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewFulfillmentRequest(@NotBlank String decision) {
}
