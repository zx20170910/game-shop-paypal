package com.unicorn.gameshop.refund.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;

@Mapper
public interface RefundMapper {

    long sumSuccessfulRefunds(@Param("orderId") String orderId);

    RefundRecord findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    int insert(@Param("id") String id,
               @Param("orderId") String orderId,
               @Param("providerRefundId") String providerRefundId,
               @Param("amountMinor") long amountMinor,
               @Param("currency") String currency,
               @Param("status") String status,
               @Param("reason") String reason,
               @Param("idempotencyKey") String idempotencyKey,
               @Param("createdAt") Instant createdAt,
               @Param("updatedAt") Instant updatedAt);

    class RefundRecord {
        private String id;
        private String providerRefundId;
        private long amountMinor;
        private String currency;
        private String status;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getProviderRefundId() { return providerRefundId; }
        public void setProviderRefundId(String providerRefundId) { this.providerRefundId = providerRefundId; }
        public long getAmountMinor() { return amountMinor; }
        public void setAmountMinor(long amountMinor) { this.amountMinor = amountMinor; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
