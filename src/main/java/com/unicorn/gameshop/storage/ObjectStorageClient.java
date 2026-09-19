package com.unicorn.gameshop.storage;

public interface ObjectStorageClient {

    UploadIntent createUploadIntent(String objectKey, String contentType, long sizeBytes);

    record UploadIntent(String provider, String objectKey, String uploadUrl, boolean configured) {
    }
}
