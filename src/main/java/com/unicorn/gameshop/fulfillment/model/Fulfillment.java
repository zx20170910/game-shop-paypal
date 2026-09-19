package com.unicorn.gameshop.fulfillment.model;

import com.unicorn.gameshop.order.model.FulfillmentStatus;

import java.time.Instant;

public class Fulfillment {

    private String id;
    private String orderId;
    private String targetPlayerUid;
    private String operatorId;
    private Instant claimedAt;
    private String proofObjectKey;
    private String deliveryNote;
    private Instant reviewedAt;
    private FulfillmentStatus status;
    private int attempts;
    private Instant deliveredAt;
    private String lastError;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getTargetPlayerUid() { return targetPlayerUid; }
    public void setTargetPlayerUid(String targetPlayerUid) { this.targetPlayerUid = targetPlayerUid; }
    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }
    public Instant getClaimedAt() { return claimedAt; }
    public void setClaimedAt(Instant claimedAt) { this.claimedAt = claimedAt; }
    public String getProofObjectKey() { return proofObjectKey; }
    public void setProofObjectKey(String proofObjectKey) { this.proofObjectKey = proofObjectKey; }
    public String getDeliveryNote() { return deliveryNote; }
    public void setDeliveryNote(String deliveryNote) { this.deliveryNote = deliveryNote; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
    public FulfillmentStatus getStatus() { return status; }
    public void setStatus(FulfillmentStatus status) { this.status = status; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
