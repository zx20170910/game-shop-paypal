package com.unicorn.gameshop.order.controller;

import com.unicorn.gameshop.admin.service.AuditService;
import com.unicorn.gameshop.common.ApiResponse;
import com.unicorn.gameshop.order.mapper.OrderMapper;
import com.unicorn.gameshop.payment.dto.PaymentAttemptResponse;
import com.unicorn.gameshop.payment.service.PaymentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class OrderAdminController {

    private final OrderMapper orderMapper;
    private final PaymentService paymentService;
    private final AuditService auditService;

    public OrderAdminController(OrderMapper orderMapper,
                                PaymentService paymentService,
                                AuditService auditService) {
        this.orderMapper = orderMapper;
        this.paymentService = paymentService;
        this.auditService = auditService;
    }

    @PostMapping("/expire-pending")
    public ApiResponse<Integer> expirePending(Authentication authentication) {
        int count = orderMapper.expirePending(Instant.now(), Instant.now());
        auditService.record(authentication, "ORDER_EXPIRE_PENDING", "ORDER", "QUEUE", Map.of("count", count));
        return new ApiResponse<>(count);
    }

    @PostMapping("/{orderNo}/retry-payment")
    public ApiResponse<PaymentAttemptResponse> retryPayment(
            @PathVariable String orderNo,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Authentication authentication) {
        PaymentAttemptResponse result = paymentService.retryPayPalOrder(orderNo, idempotencyKey);
        auditService.record(authentication, "PAYMENT_RETRY", "ORDER", orderNo, Map.of());
        return new ApiResponse<>(result);
    }
}
