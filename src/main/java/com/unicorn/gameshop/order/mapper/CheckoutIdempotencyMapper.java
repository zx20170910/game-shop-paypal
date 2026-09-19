package com.unicorn.gameshop.order.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;

@Mapper
public interface CheckoutIdempotencyMapper {

    CheckoutIdempotencyRecord findByKey(@Param("idempotencyKey") String idempotencyKey);

    int insert(@Param("idempotencyKey") String idempotencyKey,
               @Param("requestFingerprint") String requestFingerprint,
               @Param("orderId") String orderId,
               @Param("createdAt") Instant createdAt);

    class CheckoutIdempotencyRecord {
        private String idempotencyKey;
        private String requestFingerprint;
        private String orderId;

        public String getIdempotencyKey() { return idempotencyKey; }
        public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
        public String getRequestFingerprint() { return requestFingerprint; }
        public void setRequestFingerprint(String requestFingerprint) { this.requestFingerprint = requestFingerprint; }
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
    }
}
