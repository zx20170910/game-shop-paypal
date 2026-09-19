package com.unicorn.gameshop.paypal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.gameshop.common.ApiErrorCode;
import com.unicorn.gameshop.common.BusinessException;
import com.unicorn.gameshop.config.PayPalProperties;
import com.unicorn.gameshop.order.model.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RestClientPayPalClient implements PayPalClient {

    private final RestClient restClient;
    private final PayPalProperties properties;
    private final ObjectMapper objectMapper;
    private volatile CachedToken cachedToken;

    public RestClientPayPalClient(RestClient.Builder builder,
                                  PayPalProperties properties,
                                  ObjectMapper objectMapper) {
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public PayPalOrderResult createOrder(Order order, String requestId) {
        Map<String, Object> amount = Map.of(
                "currency_code", order.getCurrency(),
                "value", majorAmount(order.getAmountMinor()));
        Map<String, Object> purchaseUnit = new LinkedHashMap<>();
        purchaseUnit.put("invoice_id", order.getOrderNo());
        purchaseUnit.put("amount", amount);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("intent", "CAPTURE");
        body.put("purchase_units", java.util.List.of(purchaseUnit));

        JsonNode response = post("/v2/checkout/orders", requestId, body);
        return new PayPalOrderResult(text(response, "id"), text(response, "status"), raw(response));
    }

    @Override
    public PayPalCaptureResult capture(String paypalOrderId, String requestId) {
        JsonNode response = restClient.post()
                .uri("/v2/checkout/orders/{id}/capture", paypalOrderId)
                .headers(headers -> authorize(headers, requestId))
                .retrieve()
                .body(JsonNode.class);
        JsonNode capture = response == null ? null : response.path("purchase_units").path(0)
                .path("payments").path("captures").path(0);
        return new PayPalCaptureResult(text(capture, "id"), text(response, "status"), raw(response));
    }

    @Override
    public PayPalRefundResult refund(String captureId, long amountMinor, String currency, String requestId) {
        Map<String, String> amount = Map.of("value", majorAmount(amountMinor), "currency_code", currency);
        JsonNode response = post("/v2/payments/captures/" + captureId + "/refund", requestId,
                Map.of("amount", amount));
        return new PayPalRefundResult(text(response, "id"), text(response, "status"), raw(response));
    }

    @Override
    public boolean verifyWebhook(String rawBody, Map<String, String> headers) {
        if (properties.webhookId() == null || properties.webhookId().isBlank()) {
            return false;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("transmission_id", headers.get("PayPal-Transmission-Id"));
            body.put("transmission_time", headers.get("PayPal-Transmission-Time"));
            body.put("cert_url", headers.get("PayPal-Cert-Url"));
            body.put("auth_algo", headers.get("PayPal-Auth-Algo"));
            body.put("transmission_sig", headers.get("PayPal-Transmission-Sig"));
            body.put("webhook_id", properties.webhookId());
            body.put("webhook_event", objectMapper.readTree(rawBody));
            JsonNode response = restClient.post()
                    .uri("/v1/notifications/verify-webhook-signature")
                    .headers(this::authorize)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            return response != null && "SUCCESS".equalsIgnoreCase(text(response, "verification_status"));
        } catch (Exception exception) {
            return false;
        }
    }

    private JsonNode post(String uri, String requestId, Object body) {
        return restClient.post()
                .uri(uri)
                .headers(headers -> authorize(headers, requestId))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
    }

    private void authorize(HttpHeaders headers) {
        authorize(headers, null);
    }

    private void authorize(HttpHeaders headers, String requestId) {
        headers.setBearerAuth(accessToken());
        if (requestId != null && !requestId.isBlank()) {
            headers.set("PayPal-Request-Id", requestId);
        }
    }

    private synchronized String accessToken() {
        long now = System.currentTimeMillis();
        if (cachedToken != null && cachedToken.expiresAtMillis() > now) {
            return cachedToken.value();
        }
        if (properties.clientId() == null || properties.clientId().isBlank()
                || properties.clientSecret() == null || properties.clientSecret().isBlank()) {
            throw new BusinessException(ApiErrorCode.PAYPAL_CAPTURE_FAILED,
                    "PayPal credentials are not configured", HttpStatus.BAD_GATEWAY, true);
        }
        JsonNode response = restClient.post()
                .uri("/v1/oauth2/token")
                .headers(headers -> {
                    headers.setBasicAuth(properties.clientId(), properties.clientSecret());
                    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                })
                .body("grant_type=client_credentials")
                .retrieve()
                .body(JsonNode.class);
        String token = text(response, "access_token");
        long expiresIn = response == null ? 0 : response.path("expires_in").asLong(0);
        if (token == null || token.isBlank() || expiresIn <= 0) {
            throw new BusinessException(ApiErrorCode.PAYPAL_CAPTURE_FAILED,
                    "PayPal access token was not returned", HttpStatus.BAD_GATEWAY, true);
        }
        long cacheSeconds = Math.max(1, Math.min(properties.tokenCacheSeconds(), expiresIn - 30));
        cachedToken = new CachedToken(token, now + cacheSeconds * 1000);
        return token;
    }

    private String majorAmount(long amountMinor) {
        return BigDecimal.valueOf(amountMinor, 2).setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String raw(JsonNode node) {
        return node == null ? "{}" : node.toString();
    }

    private record CachedToken(String value, long expiresAtMillis) {
    }
}
