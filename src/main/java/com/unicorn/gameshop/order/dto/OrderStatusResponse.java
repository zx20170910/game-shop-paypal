package com.unicorn.gameshop.order.dto;

import com.unicorn.gameshop.order.model.FulfillmentStatus;
import com.unicorn.gameshop.order.model.OrderStatus;
import com.unicorn.gameshop.order.model.PaymentStatus;

import java.time.Instant;

public record OrderStatusResponse(
        String orderId,
        OrderStatus orderStatus,
        PaymentStatus paymentStatus,
        FulfillmentStatus fulfillmentStatus,
        String captureId,
        String playerUid,
        Instant deliveredAt,
        String riskStatus,
        String riskReason
) {
}
