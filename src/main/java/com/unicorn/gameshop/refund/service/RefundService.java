package com.unicorn.gameshop.refund.service;

import com.unicorn.gameshop.common.ApiErrorCode;
import com.unicorn.gameshop.common.BusinessException;
import com.unicorn.gameshop.common.IdGenerator;
import com.unicorn.gameshop.event.service.OutboxEventService;
import com.unicorn.gameshop.order.mapper.OrderMapper;
import com.unicorn.gameshop.order.model.Order;
import com.unicorn.gameshop.order.model.OrderStatus;
import com.unicorn.gameshop.order.model.PaymentStatus;
import com.unicorn.gameshop.payment.mapper.PaymentAttemptMapper;
import com.unicorn.gameshop.payment.model.PaymentAttempt;
import com.unicorn.gameshop.paypal.PayPalClient;
import com.unicorn.gameshop.refund.dto.RefundRequest;
import com.unicorn.gameshop.refund.dto.RefundResponse;
import com.unicorn.gameshop.refund.mapper.RefundMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RefundService {

    private final OrderMapper orderMapper;
    private final PaymentAttemptMapper paymentAttemptMapper;
    private final RefundMapper refundMapper;
    private final PayPalClient payPalClient;
    private final OutboxEventService outboxEventService;

    public RefundService(OrderMapper orderMapper,
                         PaymentAttemptMapper paymentAttemptMapper,
                         RefundMapper refundMapper,
                         PayPalClient payPalClient,
                         OutboxEventService outboxEventService) {
        this.orderMapper = orderMapper;
        this.paymentAttemptMapper = paymentAttemptMapper;
        this.refundMapper = refundMapper;
        this.payPalClient = payPalClient;
        this.outboxEventService = outboxEventService;
    }

    @Transactional
    public RefundResponse refund(String orderNo, RefundRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(ApiErrorCode.BAD_REQUEST, "Idempotency-Key is required");
        }
        RefundMapper.RefundRecord existing = refundMapper.findByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return new RefundResponse(existing.getId(), existing.getProviderRefundId(), existing.getAmountMinor(),
                    existing.getCurrency(), existing.getStatus());
        }
        Order order = orderMapper.findByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(ApiErrorCode.ORDER_NOT_FOUND, "Order does not exist");
        }
        if ((order.getOrderStatus() != OrderStatus.PAID && order.getOrderStatus() != OrderStatus.FULFILLED)
                || (order.getPaymentStatus() != PaymentStatus.CAPTURED
                && order.getPaymentStatus() != PaymentStatus.PARTIALLY_REFUNDED)) {
            throw new BusinessException(ApiErrorCode.REFUND_NOT_ALLOWED,
                    "Order is not in a refundable payment state");
        }
        if (!order.getCurrency().equalsIgnoreCase(request.currency())) {
            throw new BusinessException(ApiErrorCode.REFUND_NOT_ALLOWED, "Refund currency does not match order");
        }
        long refunded = refundMapper.sumSuccessfulRefunds(order.getId());
        long remaining = order.getAmountMinor() - refunded;
        if (request.amountMinor() > remaining) {
            throw new BusinessException(ApiErrorCode.REFUND_AMOUNT_EXCEEDED, "Refund amount exceeds captured balance");
        }
        PaymentAttempt paymentAttempt = paymentAttemptMapper.findByOrderId(order.getId());
        if (paymentAttempt == null || paymentAttempt.getProviderCaptureId() == null) {
            throw new BusinessException(ApiErrorCode.REFUND_NOT_ALLOWED, "PayPal capture is not available");
        }

        PayPalClient.PayPalRefundResult result = payPalClient.refund(paymentAttempt.getProviderCaptureId(),
                request.amountMinor(), order.getCurrency(), idempotencyKey);
        String status = "COMPLETED".equalsIgnoreCase(result.status()) ? "COMPLETED" : "PENDING";
        String refundId = IdGenerator.id();
        Instant now = Instant.now();
        refundMapper.insert(refundId, order.getId(), result.providerRefundId(), request.amountMinor(),
                order.getCurrency(), status, request.reason(), idempotencyKey, now, now);
        if ("COMPLETED".equals(status)) {
            PaymentStatus paymentStatus = request.amountMinor() == remaining
                    ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
            orderMapper.updatePayment(order.getId(), order.getOrderStatus(), paymentStatus,
                    order.getFulfillmentStatus(), now);
        }
        outboxEventService.record("REFUND_CREATED", "ORDER", order.getId(),
                java.util.Map.of("orderNo", order.getOrderNo(), "amountMinor", request.amountMinor(),
                        "status", status));
        return new RefundResponse(refundId, result.providerRefundId(), request.amountMinor(), order.getCurrency(), status);
    }
}
