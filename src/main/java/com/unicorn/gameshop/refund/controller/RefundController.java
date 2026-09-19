package com.unicorn.gameshop.refund.controller;

import com.unicorn.gameshop.common.ApiResponse;
import com.unicorn.gameshop.admin.service.AuditService;
import com.unicorn.gameshop.refund.dto.RefundRequest;
import com.unicorn.gameshop.refund.dto.RefundResponse;
import com.unicorn.gameshop.refund.service.RefundService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
public class RefundController {

    private final RefundService refundService;
    private final AuditService auditService;

    public RefundController(RefundService refundService, AuditService auditService) {
        this.refundService = refundService;
        this.auditService = auditService;
    }

    @PostMapping("/{orderNo}/refunds")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RefundResponse> refund(
            @PathVariable String orderNo,
            @Valid @RequestBody RefundRequest request,
            Authentication authentication,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        RefundResponse result = refundService.refund(orderNo, request, idempotencyKey);
        auditService.record(authentication, "REFUND_CREATE", "ORDER", orderNo,
                Map.of("amountMinor", request.amountMinor(), "reason", request.reason()));
        return new ApiResponse<>(result);
    }
}
