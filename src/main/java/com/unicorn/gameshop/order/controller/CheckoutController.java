package com.unicorn.gameshop.order.controller;

import com.unicorn.gameshop.common.ApiResponse;
import com.unicorn.gameshop.order.dto.CreateCheckoutSessionRequest;
import com.unicorn.gameshop.order.dto.CreateCheckoutSessionResponse;
import com.unicorn.gameshop.order.service.CheckoutService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checkout/sessions")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping
    public ApiResponse<CreateCheckoutSessionResponse> create(
            @Valid @RequestBody CreateCheckoutSessionRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return new ApiResponse<>(checkoutService.create(request, idempotencyKey));
    }
}
