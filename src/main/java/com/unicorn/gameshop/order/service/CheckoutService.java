package com.unicorn.gameshop.order.service;

import com.unicorn.gameshop.common.AccessTokenService;
import com.unicorn.gameshop.common.ApiErrorCode;
import com.unicorn.gameshop.common.BusinessException;
import com.unicorn.gameshop.common.IdGenerator;
import com.unicorn.gameshop.order.dto.CreateCheckoutSessionRequest;
import com.unicorn.gameshop.order.dto.CreateCheckoutSessionResponse;
import com.unicorn.gameshop.order.mapper.CheckoutIdempotencyMapper;
import com.unicorn.gameshop.order.mapper.OrderMapper;
import com.unicorn.gameshop.order.mapper.ProductMapper;
import com.unicorn.gameshop.order.model.FulfillmentStatus;
import com.unicorn.gameshop.order.model.Order;
import com.unicorn.gameshop.order.model.OrderStatus;
import com.unicorn.gameshop.order.model.PaymentStatus;
import com.unicorn.gameshop.order.model.Product;
import com.unicorn.gameshop.risk.service.RiskAssessmentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

@Service
public class CheckoutService {

    private static final Map<String, String> COUNTRY_CURRENCY = Map.of(
            "US", "USD",
            "GB", "GBP",
            "DE", "EUR");

    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final CheckoutIdempotencyMapper idempotencyMapper;
    private final AccessTokenService accessTokenService;
    private final RiskAssessmentService riskAssessmentService;
    private final long expirationMinutes;

    public CheckoutService(ProductMapper productMapper,
                           OrderMapper orderMapper,
                           CheckoutIdempotencyMapper idempotencyMapper,
                           AccessTokenService accessTokenService,
                           RiskAssessmentService riskAssessmentService,
                           @Value("${app.order-expiration-minutes}") long expirationMinutes) {
        this.productMapper = productMapper;
        this.orderMapper = orderMapper;
        this.idempotencyMapper = idempotencyMapper;
        this.accessTokenService = accessTokenService;
        this.riskAssessmentService = riskAssessmentService;
        this.expirationMinutes = expirationMinutes;
    }

    @Transactional
    public CreateCheckoutSessionResponse create(CreateCheckoutSessionRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(ApiErrorCode.BAD_REQUEST, "Idempotency-Key is required");
        }

        String country = request.country().trim().toUpperCase(Locale.ROOT);
        String currency = request.currency().trim().toUpperCase(Locale.ROOT);
        String fingerprint = fingerprint(request, country, currency);
        CheckoutIdempotencyMapper.CheckoutIdempotencyRecord existing = idempotencyMapper.findByKey(idempotencyKey);
        if (existing != null) {
            if (!existing.getRequestFingerprint().equals(fingerprint)) {
                throw new BusinessException(ApiErrorCode.IDEMPOTENCY_CONFLICT,
                        "The idempotency key was already used with a different request");
            }
            Order existingOrder = orderMapper.findById(existing.getOrderId());
            return response(existingOrder, accessTokenService.issue(idempotencyKey, existingOrder.getId()));
        }

        String expectedCurrency = COUNTRY_CURRENCY.get(country);
        if (expectedCurrency == null || !expectedCurrency.equals(currency)) {
            throw new BusinessException(ApiErrorCode.COUNTRY_NOT_SUPPORTED,
                    "Country and currency are not supported together");
        }

        Product product = productMapper.findAvailableById(request.productId());
        if (product == null || !product.getGameId().equals(request.gameId())) {
            throw new BusinessException(ApiErrorCode.PRODUCT_NOT_AVAILABLE, "Product is not available");
        }
        if (!product.getCurrency().equals(currency)) {
            throw new BusinessException(ApiErrorCode.PRODUCT_NOT_AVAILABLE, "Product currency is not supported");
        }

        long amountMinor;
        try {
            amountMinor = Math.multiplyExact(product.getAmountMinor(), request.quantity());
        } catch (ArithmeticException exception) {
            throw new BusinessException(ApiErrorCode.BAD_REQUEST, "Order amount is out of range");
        }
        RiskAssessmentService.RiskAssessment riskAssessment = riskAssessmentService.assess(
                product.getId(), request.playerUid().trim(), amountMinor);

        Instant now = Instant.now();
        String orderId = IdGenerator.id();
        String accessToken = accessTokenService.issue(idempotencyKey, orderId);
        Order order = new Order();
        order.setId(orderId);
        order.setOrderNo(IdGenerator.orderNo(java.time.Clock.systemUTC()));
        order.setProductId(product.getId());
        order.setGameId(product.getGameId());
        order.setPlayerUid(request.playerUid().trim());
        order.setQuantity(request.quantity());
        order.setAmountMinor(amountMinor);
        order.setCurrency(currency);
        order.setCountry(country);
        order.setAccessTokenHash(accessTokenService.hash(accessToken));
        order.setOrderStatus(OrderStatus.CREATED);
        order.setPaymentStatus(PaymentStatus.CREATED);
        order.setFulfillmentStatus(FulfillmentStatus.NOT_REQUIRED);
        order.setRiskStatus(riskAssessment.status());
        order.setRiskReason(riskAssessment.reason());
        order.setExpiresAt(now.plusSeconds(expirationMinutes * 60));
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        orderMapper.insert(order);
        try {
            idempotencyMapper.insert(idempotencyKey, fingerprint, order.getId(), now);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ApiErrorCode.IDEMPOTENCY_CONFLICT,
                    "The checkout request is being processed, please retry with the same key");
        }
        return response(order, accessToken);
    }

    public Order getById(String id) {
        Order order = orderMapper.findById(id);
        if (order == null) {
            throw new BusinessException(ApiErrorCode.ORDER_NOT_FOUND, "Order does not exist");
        }
        return order;
    }

    public Order getAuthorizedOrder(String orderNo, String accessToken) {
        Order order = orderMapper.findByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(ApiErrorCode.ORDER_NOT_FOUND, "Order does not exist");
        }
        if (accessToken == null || accessToken.isBlank()
                || !accessTokenService.matches(accessToken, order.getAccessTokenHash())) {
            throw new BusinessException(ApiErrorCode.ORDER_NOT_FOUND, "Order does not exist");
        }
        return order;
    }

    public void ensureNotExpired(Order order) {
        if (order.getExpiresAt().isBefore(Instant.now()) && order.getOrderStatus() == OrderStatus.CREATED) {
            throw new BusinessException(ApiErrorCode.ORDER_EXPIRED, "Order has expired");
        }
    }

    private CreateCheckoutSessionResponse response(Order order, String accessToken) {
        return new CreateCheckoutSessionResponse(order.getId(), order.getOrderNo(), accessToken,
                order.getAmountMinor(), order.getCurrency(), order.getExpiresAt());
    }

    private String fingerprint(CreateCheckoutSessionRequest request, String country, String currency) {
        String value = String.join("|", request.productId(), Integer.toString(request.quantity()), request.gameId(),
                request.playerUid().trim(), country, currency);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to fingerprint checkout request", exception);
        }
    }
}
