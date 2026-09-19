package com.unicorn.gameshop.risk.service;

import com.unicorn.gameshop.config.RiskProperties;
import com.unicorn.gameshop.order.mapper.OrderMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class RiskAssessmentService {

    private final OrderMapper orderMapper;
    private final RiskProperties properties;

    public RiskAssessmentService(OrderMapper orderMapper, RiskProperties properties) {
        this.orderMapper = orderMapper;
        this.properties = properties;
    }

    public RiskAssessment assess(String productId, String playerUid, long amountMinor) {
        List<String> reasons = new ArrayList<>();
        if (properties.highAmountMinor() > 0 && amountMinor >= properties.highAmountMinor()) {
            reasons.add("HIGH_AMOUNT");
        }
        Instant createdAfter = Instant.now().minusSeconds(properties.repeatWindowMinutes() * 60);
        int recentCount = orderMapper.countRecentByPlayerAndProduct(playerUid, productId, createdAfter);
        if (properties.repeatLimit() > 0 && recentCount >= properties.repeatLimit()) {
            reasons.add("REPEATED_PURCHASE");
        }
        return reasons.isEmpty()
                ? new RiskAssessment("CLEAR", null)
                : new RiskAssessment("MANUAL_REVIEW", String.join(",", reasons));
    }

    public record RiskAssessment(String status, String reason) {
    }
}
