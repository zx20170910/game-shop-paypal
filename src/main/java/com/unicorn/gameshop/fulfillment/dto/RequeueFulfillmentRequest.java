package com.unicorn.gameshop.fulfillment.dto;

import jakarta.validation.constraints.NotBlank;

public record RequeueFulfillmentRequest(@NotBlank String reason) {
}
