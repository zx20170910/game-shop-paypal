package com.unicorn.gameshop.risk.controller;

import com.unicorn.gameshop.admin.service.AuditService;
import com.unicorn.gameshop.common.ApiResponse;
import com.unicorn.gameshop.order.model.Order;
import com.unicorn.gameshop.risk.dto.RiskReviewRequest;
import com.unicorn.gameshop.risk.service.RiskReviewService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class RiskReviewController {

    private final RiskReviewService riskReviewService;
    private final AuditService auditService;

    public RiskReviewController(RiskReviewService riskReviewService, AuditService auditService) {
        this.riskReviewService = riskReviewService;
        this.auditService = auditService;
    }

    @PostMapping("/{orderNo}/risk/review")
    public ApiResponse<Order> review(@PathVariable String orderNo,
                                     @Valid @RequestBody RiskReviewRequest request,
                                     Authentication authentication) {
        Order order = riskReviewService.review(orderNo, request);
        auditService.record(authentication, "RISK_REVIEW", "ORDER", orderNo,
                Map.of("decision", request.decision()));
        return new ApiResponse<>(order);
    }
}
