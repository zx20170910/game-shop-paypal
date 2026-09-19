package com.unicorn.gameshop.payment.model;

import com.unicorn.gameshop.order.model.PaymentStatus;

import java.time.Instant;

public class PaymentAttempt {

    private String id;
    private String orderId;
    private String provider;
    private String providerOrderId;
    private String providerCaptureId;
    private long amountMinor;
    private String currency;
    private PaymentStatus status;
    private String createIdempotencyKey;
    private String captureIdempotencyKey;
    private String rawResponse;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getProviderOrderId() { return providerOrderId; }
    public void setProviderOrderId(String providerOrderId) { this.providerOrderId = providerOrderId; }
    public String getProviderCaptureId() { return providerCaptureId; }
    public void setProviderCaptureId(String providerCaptureId) { this.providerCaptureId = providerCaptureId; }
    public long getAmountMinor() { return amountMinor; }
    public void setAmountMinor(long amountMinor) { this.amountMinor = amountMinor; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public String getCreateIdempotencyKey() { return createIdempotencyKey; }
    public void setCreateIdempotencyKey(String createIdempotencyKey) { this.createIdempotencyKey = createIdempotencyKey; }
    public String getCaptureIdempotencyKey() { return captureIdempotencyKey; }
    public void setCaptureIdempotencyKey(String captureIdempotencyKey) { this.captureIdempotencyKey = captureIdempotencyKey; }
    public String getRawResponse() { return rawResponse; }
    public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
