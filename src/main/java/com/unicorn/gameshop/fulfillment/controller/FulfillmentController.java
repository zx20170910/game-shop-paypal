package com.unicorn.gameshop.fulfillment.controller;

import com.unicorn.gameshop.common.ApiResponse;
import com.unicorn.gameshop.admin.service.AuditService;
import com.unicorn.gameshop.fulfillment.dto.CompleteFulfillmentRequest;
import com.unicorn.gameshop.fulfillment.dto.ReviewFulfillmentRequest;
import com.unicorn.gameshop.fulfillment.dto.RequeueFulfillmentRequest;
import com.unicorn.gameshop.order.model.FulfillmentStatus;
import com.unicorn.gameshop.fulfillment.model.Fulfillment;
import com.unicorn.gameshop.fulfillment.service.FulfillmentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/fulfillments")
public class FulfillmentController {

    private final FulfillmentService fulfillmentService;
    private final AuditService auditService;

    public FulfillmentController(FulfillmentService fulfillmentService, AuditService auditService) {
        this.fulfillmentService = fulfillmentService;
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','REVIEWER')")
    public ApiResponse<List<Fulfillment>> list(@RequestParam(required = false) FulfillmentStatus status,
                                               @RequestParam(defaultValue = "50") int limit) {
        return new ApiResponse<>(fulfillmentService.list(status, limit));
    }

    @PostMapping("/{id}/claim")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ApiResponse<Fulfillment> claim(@PathVariable String id, Authentication authentication) {
        Fulfillment result = fulfillmentService.claim(id, authentication.getName());
        auditService.record(authentication, "FULFILLMENT_CLAIM", "FULFILLMENT", id, Map.of());
        return new ApiResponse<>(result);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ApiResponse<Fulfillment> complete(@PathVariable String id,
                                             Authentication authentication,
                                             @Valid @RequestBody CompleteFulfillmentRequest request) {
        Fulfillment result = fulfillmentService.complete(id, authentication.getName(), request);
        auditService.record(authentication, "FULFILLMENT_COMPLETE", "FULFILLMENT", id,
                Map.of("proofObjectKey", request.proofObjectKey()));
        return new ApiResponse<>(result);
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
    public ApiResponse<Fulfillment> review(@PathVariable String id,
                                           @Valid @RequestBody ReviewFulfillmentRequest request,
                                           Authentication authentication) {
        Fulfillment result = fulfillmentService.review(id, request);
        auditService.record(authentication, "FULFILLMENT_REVIEW", "FULFILLMENT", id,
                Map.of("decision", request.decision()));
        return new ApiResponse<>(result);
    }

    @PostMapping("/{id}/requeue")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Fulfillment> requeue(@PathVariable String id,
                                            @Valid @RequestBody RequeueFulfillmentRequest request,
                                            Authentication authentication) {
        Fulfillment result = fulfillmentService.requeue(id, request.reason());
        auditService.record(authentication, "FULFILLMENT_REQUEUE", "FULFILLMENT", id,
                Map.of("reason", request.reason()));
        return new ApiResponse<>(result);
    }

    @PostMapping("/reclaim-expired")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Integer> reclaimExpired(Authentication authentication) {
        int count = fulfillmentService.reclaimExpired(30);
        auditService.record(authentication, "FULFILLMENT_RECLAIM_EXPIRED", "FULFILLMENT", "QUEUE",
                Map.of("count", count));
        return new ApiResponse<>(count);
    }
}
