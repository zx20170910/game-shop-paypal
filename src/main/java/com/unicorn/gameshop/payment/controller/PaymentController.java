package com.unicorn.gameshop.payment.controller;

import com.unicorn.gameshop.common.ApiResponse;
import com.unicorn.gameshop.payment.dto.CaptureResponse;
import com.unicorn.gameshop.payment.dto.PaymentAttemptResponse;
import com.unicorn.gameshop.payment.service.PaymentService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/checkout/sessions/{sessionId}/payments/paypal/order")
    public ApiResponse<PaymentAttemptResponse> createPayPalOrder(
            @PathVariable String sessionId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return new ApiResponse<>(paymentService.createPayPalOrder(sessionId, idempotencyKey));
    }

    @PostMapping("/payments/paypal/orders/{paypalOrderId}/capture")
    public ApiResponse<CaptureResponse> capture(
            @PathVariable String paypalOrderId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return new ApiResponse<>(paymentService.capture(paypalOrderId, idempotencyKey));
    }
}
