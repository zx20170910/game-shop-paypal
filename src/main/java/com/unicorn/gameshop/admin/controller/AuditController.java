package com.unicorn.gameshop.admin.controller;

import com.unicorn.gameshop.admin.model.AuditLog;
import com.unicorn.gameshop.admin.service.AuditMapper;
import com.unicorn.gameshop.common.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/audits")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditMapper auditMapper;

    public AuditController(AuditMapper auditMapper) {
        this.auditMapper = auditMapper;
    }

    @GetMapping
    public ApiResponse<List<AuditLog>> list(@RequestParam(defaultValue = "50") int limit) {
        return new ApiResponse<>(auditMapper.listRecent(Math.min(Math.max(limit, 1), 100)));
    }
}
