package com.unicorn.gameshop.storage;

import org.springframework.stereotype.Component;

@Component
public class UnconfiguredObjectStorageClient implements ObjectStorageClient {

    @Override
    public UploadIntent createUploadIntent(String objectKey, String contentType, long sizeBytes) {
        return new UploadIntent("UNCONFIGURED", objectKey, null, false);
    }
}
