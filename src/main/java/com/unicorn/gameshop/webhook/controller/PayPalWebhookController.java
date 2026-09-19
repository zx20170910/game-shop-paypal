package com.unicorn.gameshop.webhook.controller;

import com.unicorn.gameshop.common.ApiResponse;
import com.unicorn.gameshop.webhook.service.PayPalWebhookService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks/paypal")
public class PayPalWebhookController {

    private final PayPalWebhookService webhookService;

    public PayPalWebhookController(PayPalWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping
    public ApiResponse<Boolean> receive(
            @RequestBody String rawBody,
            @RequestHeader(value = "PayPal-Transmission-Id", required = false) String transmissionId,
            @RequestHeader(value = "PayPal-Transmission-Time", required = false) String transmissionTime,
            @RequestHeader(value = "PayPal-Cert-Url", required = false) String certUrl,
            @RequestHeader(value = "PayPal-Auth-Algo", required = false) String authAlgo,
            @RequestHeader(value = "PayPal-Transmission-Sig", required = false) String transmissionSignature) {
        Map<String, String> headers = new HashMap<>();
        headers.put("PayPal-Transmission-Id", transmissionId);
        headers.put("PayPal-Transmission-Time", transmissionTime);
        headers.put("PayPal-Cert-Url", certUrl);
        headers.put("PayPal-Auth-Algo", authAlgo);
        headers.put("PayPal-Transmission-Sig", transmissionSignature);
        return new ApiResponse<>(webhookService.receive(rawBody, headers));
    }
}
