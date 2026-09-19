package com.unicorn.gameshop.paypal;

import com.unicorn.gameshop.order.model.Order;

import java.util.Map;

public interface PayPalClient {

    PayPalOrderResult createOrder(Order order, String requestId);

    PayPalCaptureResult capture(String paypalOrderId, String requestId);

    PayPalRefundResult refund(String captureId, long amountMinor, String currency, String requestId);

    boolean verifyWebhook(String rawBody, Map<String, String> headers);

    record PayPalOrderResult(String providerOrderId, String status, String rawBody) {
    }

    record PayPalCaptureResult(String providerCaptureId, String status, String rawBody) {
    }

    record PayPalRefundResult(String providerRefundId, String status, String rawBody) {
    }
}
