package com.unicorn.gameshop.payment.service;

import com.unicorn.gameshop.common.ApiErrorCode;
import com.unicorn.gameshop.common.BusinessException;
import com.unicorn.gameshop.common.IdGenerator;
import com.unicorn.gameshop.event.service.OutboxEventService;
import com.unicorn.gameshop.fulfillment.service.FulfillmentService;
import com.unicorn.gameshop.order.mapper.OrderMapper;
import com.unicorn.gameshop.order.model.FulfillmentStatus;
import com.unicorn.gameshop.order.model.Order;
import com.unicorn.gameshop.order.model.OrderStatus;
import com.unicorn.gameshop.order.model.PaymentStatus;
import com.unicorn.gameshop.order.service.CheckoutService;
import com.unicorn.gameshop.payment.dto.CaptureResponse;
import com.unicorn.gameshop.payment.dto.PaymentAttemptResponse;
import com.unicorn.gameshop.payment.mapper.PaymentAttemptMapper;
import com.unicorn.gameshop.payment.model.PaymentAttempt;
import com.unicorn.gameshop.paypal.PayPalClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class PaymentService {

    private static final String PROVIDER = "PAYPAL";

    private final CheckoutService checkoutService;
    private final OrderMapper orderMapper;
    private final PaymentAttemptMapper paymentAttemptMapper;
    private final FulfillmentService fulfillmentService;
    private final PayPalClient payPalClient;
    private final OutboxEventService outboxEventService;

    public PaymentService(CheckoutService checkoutService,
                          OrderMapper orderMapper,
                          PaymentAttemptMapper paymentAttemptMapper,
                          FulfillmentService fulfillmentService,
                          PayPalClient payPalClient,
                          OutboxEventService outboxEventService) {
        this.checkoutService = checkoutService;
        this.orderMapper = orderMapper;
        this.paymentAttemptMapper = paymentAttemptMapper;
        this.fulfillmentService = fulfillmentService;
        this.payPalClient = payPalClient;
        this.outboxEventService = outboxEventService;
    }

    @Transactional
    public PaymentAttemptResponse createPayPalOrder(String sessionId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(ApiErrorCode.BAD_REQUEST, "Idempotency-Key is required");
        }
        Order order = checkoutService.getById(sessionId);
        checkoutService.ensureNotExpired(order);
        PaymentAttempt idempotentAttempt = paymentAttemptMapper.findByCreateIdempotencyKey(idempotencyKey);
        if (idempotentAttempt != null) {
            if (!idempotentAttempt.getOrderId().equals(order.getId())) {
                throw new BusinessException(ApiErrorCode.IDEMPOTENCY_CONFLICT,
                        "The payment idempotency key belongs to another order");
            }
            if (idempotentAttempt.getProviderOrderId() != null) {
                return new PaymentAttemptResponse(idempotentAttempt.getId(), PROVIDER,
                        idempotentAttempt.getProviderOrderId(), idempotentAttempt.getStatus().name());
            }
        }
        PaymentAttempt existing = paymentAttemptMapper.findByOrderId(order.getId());
        if (existing != null && existing.getProviderOrderId() != null) {
            return new PaymentAttemptResponse(existing.getId(), PROVIDER, existing.getProviderOrderId(),
                    existing.getStatus().name());
        }

        Instant now = Instant.now();
        PaymentAttempt attempt = paymentAttemptMapper.findByCreateIdempotencyKey(idempotencyKey);
        if (attempt == null) {
            attempt = new PaymentAttempt();
            attempt.setId(IdGenerator.id());
            attempt.setOrderId(order.getId());
            attempt.setProvider(PROVIDER);
            attempt.setAmountMinor(order.getAmountMinor());
            attempt.setCurrency(order.getCurrency());
            attempt.setStatus(PaymentStatus.CAPTURE_PENDING);
            attempt.setCreateIdempotencyKey(idempotencyKey);
            attempt.setCreatedAt(now);
            attempt.setUpdatedAt(now);
            paymentAttemptMapper.insert(attempt);
        }

        PayPalClient.PayPalOrderResult result = payPalClient.createOrder(order, idempotencyKey);
        if (result.providerOrderId() == null || result.providerOrderId().isBlank()) {
            throw new BusinessException(ApiErrorCode.PAYPAL_CAPTURE_FAILED,
                    "PayPal order id was not returned", org.springframework.http.HttpStatus.BAD_GATEWAY, true);
        }
        paymentAttemptMapper.updateProviderOrder(attempt.getId(), result.providerOrderId(),
                PaymentStatus.PAYPAL_ORDER_CREATED, result.rawBody(), Instant.now());
        orderMapper.updatePayment(order.getId(), OrderStatus.CREATED, PaymentStatus.PAYPAL_ORDER_CREATED,
                FulfillmentStatus.NOT_REQUIRED, Instant.now());
        return new PaymentAttemptResponse(attempt.getId(), PROVIDER, result.providerOrderId(),
                PaymentStatus.PAYPAL_ORDER_CREATED.name());
    }

    public PaymentAttemptResponse retryPayPalOrder(String orderNo, String idempotencyKey) {
        Order order = orderMapper.findByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(ApiErrorCode.ORDER_NOT_FOUND, "Order does not exist");
        }
        if (order.getOrderStatus() != OrderStatus.FAILED) {
            throw new BusinessException(ApiErrorCode.PAYPAL_CAPTURE_NOT_ALLOWED,
                    "Only failed orders can retry payment");
        }
        return createPayPalOrder(order.getId(), idempotencyKey);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public CaptureResponse capture(String paypalOrderId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(ApiErrorCode.BAD_REQUEST, "Idempotency-Key is required");
        }
        PaymentAttempt attempt = paymentAttemptMapper.findByProviderOrderId(PROVIDER, paypalOrderId);
        if (attempt == null) {
            throw new BusinessException(ApiErrorCode.PAYMENT_ATTEMPT_NOT_FOUND, "Payment attempt does not exist");
        }
        Order order = checkoutService.getById(attempt.getOrderId());
        checkoutService.ensureNotExpired(order);
        if (attempt.getStatus() == PaymentStatus.CAPTURED) {
            return new CaptureResponse(order.getOrderNo(), attempt.getStatus().name(),
                    order.getFulfillmentStatus().name(), attempt.getProviderCaptureId());
        }
        if (attempt.getCaptureIdempotencyKey() != null
                && !attempt.getCaptureIdempotencyKey().equals(idempotencyKey)) {
            throw new BusinessException(ApiErrorCode.IDEMPOTENCY_CONFLICT,
                    "The payment capture is already being processed with another idempotency key");
        }
        if (order.getOrderStatus() != OrderStatus.CREATED
                || (attempt.getStatus() != PaymentStatus.PAYPAL_ORDER_CREATED
                && attempt.getStatus() != PaymentStatus.CAPTURE_PENDING)) {
            throw new BusinessException(ApiErrorCode.PAYPAL_CAPTURE_NOT_ALLOWED, "Payment capture is not allowed");
        }

        PayPalClient.PayPalCaptureResult result = payPalClient.capture(paypalOrderId, idempotencyKey);
        if (!"COMPLETED".equalsIgnoreCase(result.status())
                || result.providerCaptureId() == null || result.providerCaptureId().isBlank()) {
            paymentAttemptMapper.updateCapture(attempt.getId(), result.providerCaptureId(),
                    PaymentStatus.CAPTURE_FAILED, idempotencyKey, result.rawBody(), Instant.now());
            orderMapper.updatePayment(order.getId(), OrderStatus.FAILED, PaymentStatus.CAPTURE_FAILED,
                    FulfillmentStatus.NOT_REQUIRED, Instant.now());
            throw new BusinessException(ApiErrorCode.PAYPAL_CAPTURE_FAILED,
                    "The payment could not be captured", org.springframework.http.HttpStatus.BAD_GATEWAY, true);
        }

        paymentAttemptMapper.updateCapture(attempt.getId(), result.providerCaptureId(), PaymentStatus.CAPTURED,
                idempotencyKey, result.rawBody(), Instant.now());
        orderMapper.updatePayment(order.getId(), OrderStatus.PAID, PaymentStatus.CAPTURED,
                FulfillmentStatus.MANUAL_PENDING, Instant.now());
        fulfillmentService.createIfAbsent(order);
        outboxEventService.record("PAYMENT_CAPTURED", "ORDER", order.getId(),
                java.util.Map.of("orderNo", order.getOrderNo(), "captureId", result.providerCaptureId()));
        return new CaptureResponse(order.getOrderNo(), PaymentStatus.CAPTURED.name(),
                FulfillmentStatus.MANUAL_PENDING.name(), result.providerCaptureId());
    }

    @Transactional
    public void processWebhookCapture(String providerOrderId, String captureId, String rawBody) {
        PaymentAttempt attempt = paymentAttemptMapper.findByProviderOrderId(PROVIDER, providerOrderId);
        if (attempt == null || attempt.getStatus() == PaymentStatus.CAPTURED) {
            return;
        }
        Order order = checkoutService.getById(attempt.getOrderId());
        paymentAttemptMapper.updateCapture(attempt.getId(), captureId, PaymentStatus.CAPTURED,
                attempt.getCaptureIdempotencyKey(), rawBody, Instant.now());
        orderMapper.updatePayment(order.getId(), OrderStatus.PAID, PaymentStatus.CAPTURED,
                FulfillmentStatus.MANUAL_PENDING, Instant.now());
        fulfillmentService.createIfAbsent(order);
        outboxEventService.record("PAYMENT_CAPTURED", "ORDER", order.getId(),
                java.util.Map.of("orderNo", order.getOrderNo(), "captureId", captureId));
    }

    @Transactional
    public void processWebhookFailure(String providerOrderId, String rawBody) {
        PaymentAttempt attempt = paymentAttemptMapper.findByProviderOrderId(PROVIDER, providerOrderId);
        if (attempt == null || attempt.getStatus() == PaymentStatus.CAPTURED) {
            return;
        }
        paymentAttemptMapper.updateCapture(attempt.getId(), null, PaymentStatus.CAPTURE_FAILED,
                attempt.getCaptureIdempotencyKey(), rawBody, Instant.now());
        orderMapper.updatePayment(attempt.getOrderId(), OrderStatus.FAILED, PaymentStatus.CAPTURE_FAILED,
                FulfillmentStatus.NOT_REQUIRED, Instant.now());
    }

    @Transactional
    public void processWebhookRefund(String providerCaptureId, String rawBody) {
        PaymentAttempt attempt = paymentAttemptMapper.findByProviderCaptureId(PROVIDER, providerCaptureId);
        if (attempt == null) {
            return;
        }
        Order order = checkoutService.getById(attempt.getOrderId());
        if (order.getPaymentStatus() == PaymentStatus.REFUNDED) {
            return;
        }
        orderMapper.updatePayment(order.getId(), order.getOrderStatus(), PaymentStatus.REFUNDED,
                order.getFulfillmentStatus(), Instant.now());
        paymentAttemptMapper.updateCapture(attempt.getId(), attempt.getProviderCaptureId(), PaymentStatus.REFUNDED,
                attempt.getCaptureIdempotencyKey(), rawBody, Instant.now());
        outboxEventService.record("PAYMENT_REFUNDED", "ORDER", order.getId(),
                java.util.Map.of("orderNo", order.getOrderNo(), "captureId", providerCaptureId));
    }

    @Transactional
    public void processWebhookDispute(String providerCaptureId, String rawBody) {
        PaymentAttempt attempt = paymentAttemptMapper.findByProviderCaptureId(PROVIDER, providerCaptureId);
        if (attempt == null) {
            return;
        }
        Order order = checkoutService.getById(attempt.getOrderId());
        orderMapper.updatePayment(order.getId(), order.getOrderStatus(), PaymentStatus.DISPUTED,
                order.getFulfillmentStatus(), Instant.now());
        paymentAttemptMapper.updateCapture(attempt.getId(), attempt.getProviderCaptureId(), PaymentStatus.DISPUTED,
                attempt.getCaptureIdempotencyKey(), rawBody, Instant.now());
        outboxEventService.record("PAYMENT_DISPUTED", "ORDER", order.getId(),
                java.util.Map.of("orderNo", order.getOrderNo(), "captureId", providerCaptureId));
    }
}
