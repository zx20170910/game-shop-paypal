package com.unicorn.gameshop.reconciliation.controller;

import com.unicorn.gameshop.common.ApiResponse;
import com.unicorn.gameshop.reconciliation.dto.ReconciliationSummary;
import com.unicorn.gameshop.reconciliation.service.ReconciliationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reconciliation")
@PreAuthorize("hasRole('ADMIN')")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @GetMapping("/summary")
    public ApiResponse<ReconciliationSummary> summary() {
        return new ApiResponse<>(reconciliationService.summary());
    }
}
