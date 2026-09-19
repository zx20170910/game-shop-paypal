package com.unicorn.gameshop.storage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateAttachmentRequest(
        @NotBlank String fileName,
        @NotBlank String contentType,
        @Positive long sizeBytes
) {
}
