package com.unicorn.gameshop.webhook.controller;

import com.unicorn.gameshop.admin.service.AuditService;
import com.unicorn.gameshop.common.ApiResponse;
import com.unicorn.gameshop.webhook.model.WebhookEvent;
import com.unicorn.gameshop.webhook.service.PayPalWebhookService;
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
@RequestMapping("/api/v1/admin/webhooks/paypal")
@PreAuthorize("hasRole('ADMIN')")
public class PayPalWebhookAdminController {

    private final PayPalWebhookService webhookService;
    private final AuditService auditService;

    public PayPalWebhookAdminController(PayPalWebhookService webhookService, AuditService auditService) {
        this.webhookService = webhookService;
        this.auditService = auditService;
    }

    @GetMapping
    public ApiResponse<List<WebhookEvent>> list(@RequestParam(defaultValue = "50") int limit) {
        return new ApiResponse<>(webhookService.listRecent(limit));
    }

    @PostMapping("/{providerEventId}/replay")
    public ApiResponse<Boolean> replay(@PathVariable String providerEventId, Authentication authentication) {
        boolean result = webhookService.replay(providerEventId);
        auditService.record(authentication, "PAYPAL_WEBHOOK_REPLAY", "WEBHOOK", providerEventId, Map.of());
        return new ApiResponse<>(result);
    }
}
