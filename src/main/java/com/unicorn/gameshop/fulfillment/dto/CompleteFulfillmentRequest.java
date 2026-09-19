package com.unicorn.gameshop.fulfillment.dto;

import jakarta.validation.constraints.NotBlank;

public record CompleteFulfillmentRequest(
        @NotBlank String proofObjectKey,
        @NotBlank String deliveryNote
) {
}
