package com.unicorn.gameshop.order.service;

import com.unicorn.gameshop.fulfillment.mapper.FulfillmentMapper;
import com.unicorn.gameshop.fulfillment.model.Fulfillment;
import com.unicorn.gameshop.order.dto.OrderStatusResponse;
import com.unicorn.gameshop.order.model.Order;
import com.unicorn.gameshop.payment.mapper.PaymentAttemptMapper;
import com.unicorn.gameshop.payment.model.PaymentAttempt;
import org.springframework.stereotype.Service;

@Service
public class OrderQueryService {

    private final CheckoutService checkoutService;
    private final PaymentAttemptMapper paymentAttemptMapper;
    private final FulfillmentMapper fulfillmentMapper;

    public OrderQueryService(CheckoutService checkoutService,
                             PaymentAttemptMapper paymentAttemptMapper,
                             FulfillmentMapper fulfillmentMapper) {
        this.checkoutService = checkoutService;
        this.paymentAttemptMapper = paymentAttemptMapper;
        this.fulfillmentMapper = fulfillmentMapper;
    }

    public OrderStatusResponse status(String orderNo, String accessToken) {
        Order order = checkoutService.getAuthorizedOrder(orderNo, accessToken);
        PaymentAttempt paymentAttempt = paymentAttemptMapper.findByOrderId(order.getId());
        Fulfillment fulfillment = fulfillmentMapper.findByOrderId(order.getId());
        return new OrderStatusResponse(order.getOrderNo(), order.getOrderStatus(), order.getPaymentStatus(),
                order.getFulfillmentStatus(), paymentAttempt == null ? null : paymentAttempt.getProviderCaptureId(),
                order.getPlayerUid(), fulfillment == null ? null : fulfillment.getDeliveredAt(),
                order.getRiskStatus(), order.getRiskReason());
    }
}
