package com.unicorn.gameshop.storage.controller;

import com.unicorn.gameshop.admin.service.AuditService;
import com.unicorn.gameshop.common.ApiResponse;
import com.unicorn.gameshop.storage.dto.CreateAttachmentRequest;
import com.unicorn.gameshop.storage.dto.UploadIntentResponse;
import com.unicorn.gameshop.storage.model.FulfillmentAttachment;
import com.unicorn.gameshop.storage.service.FulfillmentAttachmentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/fulfillments/{fulfillmentId}/attachments")
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
public class FulfillmentAttachmentController {

    private final FulfillmentAttachmentService attachmentService;
    private final AuditService auditService;

    public FulfillmentAttachmentController(FulfillmentAttachmentService attachmentService,
                                           AuditService auditService) {
        this.attachmentService = attachmentService;
        this.auditService = auditService;
    }

    @PostMapping("/upload-intent")
    public ApiResponse<UploadIntentResponse> createIntent(
            @PathVariable String fulfillmentId,
            @Valid @RequestBody CreateAttachmentRequest request,
            Authentication authentication) {
        return new ApiResponse<>(attachmentService.createIntent(fulfillmentId, authentication.getName(), request));
    }

    @PostMapping("/{attachmentId}/confirm")
    public ApiResponse<FulfillmentAttachment> confirmUploaded(@PathVariable String fulfillmentId,
                                                              @PathVariable String attachmentId,
                                                              Authentication authentication) {
        FulfillmentAttachment result = attachmentService.confirmUploaded(fulfillmentId, attachmentId,
                authentication.getName());
        auditService.record(authentication, "FULFILLMENT_ATTACHMENT_CONFIRM", "FULFILLMENT_ATTACHMENT",
                attachmentId, java.util.Map.of("fulfillmentId", fulfillmentId));
        return new ApiResponse<>(result);
    }

    @GetMapping
    public ApiResponse<List<FulfillmentAttachment>> list(@PathVariable String fulfillmentId) {
        return new ApiResponse<>(attachmentService.list(fulfillmentId));
    }
}
