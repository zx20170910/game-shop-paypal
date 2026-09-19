package com.unicorn.gameshop.reconciliation.service;

import com.unicorn.gameshop.event.service.OutboxEventService;
import com.unicorn.gameshop.reconciliation.dto.ReconciliationSummary;
import com.unicorn.gameshop.reconciliation.mapper.ReconciliationMapper;
import org.springframework.stereotype.Service;

@Service
public class ReconciliationService {

    private final ReconciliationMapper reconciliationMapper;
    private final OutboxEventService outboxEventService;

    public ReconciliationService(ReconciliationMapper reconciliationMapper,
                                 OutboxEventService outboxEventService) {
        this.reconciliationMapper = reconciliationMapper;
        this.outboxEventService = outboxEventService;
    }

    public ReconciliationSummary summary() {
        return new ReconciliationSummary(
                reconciliationMapper.countCapturedWithoutFulfillment(),
                reconciliationMapper.countFulfilledWithoutCapturedPayment(),
                reconciliationMapper.countPaymentCaptureWithoutPaidOrder(),
                reconciliationMapper.countPendingWebhookEvents(),
                outboxEventService.count("PENDING"),
                reconciliationMapper.sumCapturedWithoutFulfillmentAmountMinor(),
                reconciliationMapper.sumFulfilledWithoutCapturedPaymentAmountMinor(),
                reconciliationMapper.sumRefundedAmountMinor());
    }
}
