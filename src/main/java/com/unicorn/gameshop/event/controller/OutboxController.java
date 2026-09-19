package com.unicorn.gameshop.event.controller;

import com.unicorn.gameshop.admin.service.AuditService;
import com.unicorn.gameshop.common.ApiResponse;
import com.unicorn.gameshop.event.model.OutboxEvent;
import com.unicorn.gameshop.event.service.OutboxEventService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/outbox")
@PreAuthorize("hasRole('ADMIN')")
public class OutboxController {

    private final OutboxEventService outboxEventService;
    private final AuditService auditService;

    public OutboxController(OutboxEventService outboxEventService, AuditService auditService) {
        this.outboxEventService = outboxEventService;
        this.auditService = auditService;
    }

    @GetMapping
    public ApiResponse<List<OutboxEvent>> list(@RequestParam(required = false) String status,
                                               @RequestParam(defaultValue = "50") int limit) {
        return new ApiResponse<>(outboxEventService.list(status, limit));
    }

    @PostMapping("/{id}/retry")
    public ApiResponse<Boolean> retry(@PathVariable String id, Authentication authentication) {
        outboxEventService.markRetryable(id, "MANUAL_RETRY");
        auditService.record(authentication, "OUTBOX_RETRY", "OUTBOX_EVENT", id, Map.of());
        return new ApiResponse<>(true);
    }
}
