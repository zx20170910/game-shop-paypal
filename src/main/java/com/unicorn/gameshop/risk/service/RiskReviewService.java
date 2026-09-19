package com.unicorn.gameshop.risk.service;

import com.unicorn.gameshop.common.ApiErrorCode;
import com.unicorn.gameshop.common.BusinessException;
import com.unicorn.gameshop.order.mapper.OrderMapper;
import com.unicorn.gameshop.order.model.FulfillmentStatus;
import com.unicorn.gameshop.order.model.Order;
import com.unicorn.gameshop.risk.dto.RiskReviewRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RiskReviewService {

    private final OrderMapper orderMapper;

    public RiskReviewService(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Transactional
    public Order review(String orderNo, RiskReviewRequest request) {
        Order order = orderMapper.findByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(ApiErrorCode.ORDER_NOT_FOUND, "Order does not exist");
        }
        String decision = request.decision().trim().toUpperCase();
        String riskStatus;
        FulfillmentStatus fulfillmentStatus;
        if ("APPROVE".equals(decision)) {
            riskStatus = "APPROVED";
            fulfillmentStatus = order.getFulfillmentStatus() == FulfillmentStatus.MANUAL_REVIEW
                    ? FulfillmentStatus.MANUAL_PENDING : order.getFulfillmentStatus();
        } else if ("REJECT".equals(decision)) {
            riskStatus = "REJECTED";
            fulfillmentStatus = FulfillmentStatus.MANUAL_REVIEW;
        } else {
            throw new BusinessException(ApiErrorCode.BAD_REQUEST, "Risk decision must be APPROVE or REJECT");
        }
        orderMapper.updateRisk(order.getId(), riskStatus,
                request.reason() == null ? order.getRiskReason() : request.reason(), fulfillmentStatus, Instant.now());
        order.setRiskStatus(riskStatus);
        order.setRiskReason(request.reason());
        order.setFulfillmentStatus(fulfillmentStatus);
        return order;
    }
}
