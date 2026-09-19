package com.unicorn.gameshop.payment.mapper;

import com.unicorn.gameshop.payment.model.PaymentAttempt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;

@Mapper
public interface PaymentAttemptMapper {

    int insert(PaymentAttempt attempt);

    PaymentAttempt findById(@Param("id") String id);

    PaymentAttempt findByProviderOrderId(@Param("provider") String provider,
                                         @Param("providerOrderId") String providerOrderId);

    PaymentAttempt findByOrderId(@Param("orderId") String orderId);

    PaymentAttempt findByCreateIdempotencyKey(@Param("createIdempotencyKey") String createIdempotencyKey);

    PaymentAttempt findByProviderCaptureId(@Param("provider") String provider,
                                           @Param("providerCaptureId") String providerCaptureId);

    int updateProviderOrder(@Param("id") String id,
                            @Param("providerOrderId") String providerOrderId,
                            @Param("status") com.unicorn.gameshop.order.model.PaymentStatus status,
                            @Param("rawResponse") String rawResponse,
                            @Param("updatedAt") Instant updatedAt);

    int updateCapture(@Param("id") String id,
                      @Param("providerCaptureId") String providerCaptureId,
                      @Param("status") com.unicorn.gameshop.order.model.PaymentStatus status,
                      @Param("captureIdempotencyKey") String captureIdempotencyKey,
                      @Param("rawResponse") String rawResponse,
                      @Param("updatedAt") Instant updatedAt);
}
