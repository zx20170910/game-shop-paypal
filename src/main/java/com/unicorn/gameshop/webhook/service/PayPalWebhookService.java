package com.unicorn.gameshop.webhook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.gameshop.common.ApiErrorCode;
import com.unicorn.gameshop.common.BusinessException;
import com.unicorn.gameshop.common.IdGenerator;
import com.unicorn.gameshop.payment.service.PaymentService;
import com.unicorn.gameshop.paypal.PayPalClient;
import com.unicorn.gameshop.webhook.mapper.WebhookEventMapper;
import com.unicorn.gameshop.webhook.model.WebhookEvent;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class PayPalWebhookService {

    private static final String PROVIDER = "PAYPAL";

    private final PayPalClient payPalClient;
    private final WebhookEventMapper webhookEventMapper;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public PayPalWebhookService(PayPalClient payPalClient,
                                WebhookEventMapper webhookEventMapper,
                                PaymentService paymentService,
                                ObjectMapper objectMapper) {
        this.payPalClient = payPalClient;
        this.webhookEventMapper = webhookEventMapper;
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    public boolean receive(String rawBody, Map<String, String> headers) {
        if (!payPalClient.verifyWebhook(rawBody, headers)) {
            throw new BusinessException(ApiErrorCode.PAYPAL_WEBHOOK_SIGNATURE_INVALID,
                    "PayPal webhook signature is invalid", HttpStatus.UNAUTHORIZED, false);
        }
        try {
            JsonNode event = objectMapper.readTree(rawBody);
            String eventId = event.path("id").asText(null);
            String eventType = event.path("event_type").asText(null);
            if (eventId == null || eventType == null) {
                throw new BusinessException(ApiErrorCode.BAD_REQUEST, "PayPal webhook payload is incomplete");
            }
            if (webhookEventMapper.countByProviderEventId(PROVIDER, eventId) > 0) {
                return true;
            }
            String headersJson = objectMapper.writeValueAsString(headers);
            webhookEventMapper.insert(IdGenerator.id(), PROVIDER, eventId, eventType, rawBody,
                    true, "RECEIVED", headersJson, Instant.now());
            processEvent(event, rawBody);
            webhookEventMapper.markProcessed(PROVIDER, eventId, Instant.now());
            return true;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ApiErrorCode.BAD_REQUEST,
                    "PayPal webhook payload is invalid");
        }
    }

    public boolean replay(String providerEventId) {
        WebhookEvent eventRecord = webhookEventMapper.findByProviderEventId(PROVIDER, providerEventId);
        if (eventRecord == null || !eventRecord.isSignatureVerified()) {
            throw new BusinessException(ApiErrorCode.BAD_REQUEST, "Verified webhook event does not exist");
        }
        try {
            JsonNode event = objectMapper.readTree(eventRecord.getRawBody());
            processEvent(event, eventRecord.getRawBody());
            webhookEventMapper.markProcessed(PROVIDER, providerEventId, Instant.now());
            return true;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ApiErrorCode.BAD_REQUEST, "Stored webhook event is invalid");
        }
    }

    public java.util.List<WebhookEvent> listRecent(int limit) {
        return webhookEventMapper.listRecent(Math.min(Math.max(limit, 1), 100));
    }

    private void processEvent(JsonNode event, String rawBody) {
        String eventType = event.path("event_type").asText(null);
        JsonNode resource = event.path("resource");
        String providerOrderId = resource.path("supplementary_data").path("related_ids")
                .path("order_id").asText(null);
        String captureId = resource.path("id").asText(null);
        switch (eventType) {
            case "PAYMENT.CAPTURE.COMPLETED" -> paymentService.processWebhookCapture(providerOrderId, captureId, rawBody);
            case "PAYMENT.CAPTURE.DENIED", "PAYMENT.CAPTURE.DECLINED" -> paymentService.processWebhookFailure(providerOrderId, rawBody);
            case "PAYMENT.CAPTURE.REFUNDED" -> paymentService.processWebhookRefund(
                    resource.path("supplementary_data").path("related_ids").path("capture_id").asText(null), rawBody);
            case "CUSTOMER.DISPUTE.CREATED", "CUSTOMER.DISPUTE.RESOLVED" -> paymentService.processWebhookDispute(
                    resource.path("disputed_transactions").path(0).path("seller_transaction_id").asText(null), rawBody);
            default -> {
                // Refund and dispute events remain in the inbox for later workflow handling.
            }
        }
    }
}
