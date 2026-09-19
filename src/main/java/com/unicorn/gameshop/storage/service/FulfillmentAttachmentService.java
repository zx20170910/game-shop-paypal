package com.unicorn.gameshop.storage.service;

import com.unicorn.gameshop.common.ApiErrorCode;
import com.unicorn.gameshop.common.BusinessException;
import com.unicorn.gameshop.common.IdGenerator;
import com.unicorn.gameshop.fulfillment.mapper.FulfillmentMapper;
import com.unicorn.gameshop.storage.ObjectStorageClient;
import com.unicorn.gameshop.storage.dto.CreateAttachmentRequest;
import com.unicorn.gameshop.storage.dto.UploadIntentResponse;
import com.unicorn.gameshop.storage.mapper.FulfillmentAttachmentMapper;
import com.unicorn.gameshop.storage.model.FulfillmentAttachment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class FulfillmentAttachmentService {

    private final FulfillmentMapper fulfillmentMapper;
    private final FulfillmentAttachmentMapper attachmentMapper;
    private final ObjectStorageClient objectStorageClient;

    public FulfillmentAttachmentService(FulfillmentMapper fulfillmentMapper,
                                        FulfillmentAttachmentMapper attachmentMapper,
                                        ObjectStorageClient objectStorageClient) {
        this.fulfillmentMapper = fulfillmentMapper;
        this.attachmentMapper = attachmentMapper;
        this.objectStorageClient = objectStorageClient;
    }

    public UploadIntentResponse createIntent(String fulfillmentId,
                                             String operatorId,
                                             CreateAttachmentRequest request) {
        if (fulfillmentMapper.findById(fulfillmentId) == null) {
            throw new BusinessException(ApiErrorCode.FULFILLMENT_NOT_FOUND, "Fulfillment does not exist");
        }
        String safeFileName = request.fileName().replaceAll("[^A-Za-z0-9._-]", "_");
        String objectKey = "fulfillment/" + fulfillmentId + "/" + IdGenerator.id() + "-" + safeFileName;
        ObjectStorageClient.UploadIntent intent = objectStorageClient.createUploadIntent(objectKey,
                request.contentType(), request.sizeBytes());
        FulfillmentAttachment attachment = new FulfillmentAttachment();
        attachment.setId(IdGenerator.id());
        attachment.setFulfillmentId(fulfillmentId);
        attachment.setObjectKey(objectKey);
        attachment.setFileName(request.fileName());
        attachment.setContentType(request.contentType());
        attachment.setSizeBytes(request.sizeBytes());
        attachment.setStatus("PENDING_UPLOAD");
        attachment.setCreatedBy(operatorId);
        attachment.setCreatedAt(Instant.now());
        attachmentMapper.insert(attachment);
        return new UploadIntentResponse(attachment.getId(), intent.provider(), intent.objectKey(),
                intent.uploadUrl(), intent.configured());
    }

    public List<FulfillmentAttachment> list(String fulfillmentId) {
        if (fulfillmentMapper.findById(fulfillmentId) == null) {
            throw new BusinessException(ApiErrorCode.FULFILLMENT_NOT_FOUND, "Fulfillment does not exist");
        }
        return attachmentMapper.listByFulfillmentId(fulfillmentId);
    }

    @Transactional
    public FulfillmentAttachment confirmUploaded(String fulfillmentId, String attachmentId, String operatorId) {
        if (fulfillmentMapper.findById(fulfillmentId) == null) {
            throw new BusinessException(ApiErrorCode.FULFILLMENT_NOT_FOUND, "Fulfillment does not exist");
        }
        if (attachmentMapper.confirmUploaded(attachmentId, fulfillmentId, operatorId, Instant.now()) != 1) {
            throw new BusinessException(ApiErrorCode.FULFILLMENT_NOT_ALLOWED,
                    "Attachment does not belong to the fulfillment or is already confirmed");
        }
        return attachmentMapper.findById(attachmentId);
    }
}
