package com.unicorn.gameshop.storage.dto;

public record UploadIntentResponse(
        String attachmentId,
        String provider,
        String objectKey,
        String uploadUrl,
        boolean configured
) {
}
