package com.unicorn.gameshop.reconciliation.mapper;

import com.unicorn.gameshop.reconciliation.dto.ReconciliationAmount;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ReconciliationMapper {

    long countCapturedWithoutFulfillment();

    long countFulfilledWithoutCapturedPayment();

    long countPaymentCaptureWithoutPaidOrder();

    long countPendingWebhookEvents();

    List<ReconciliationAmount> sumCapturedWithoutFulfillmentAmountMinor();

    List<ReconciliationAmount> sumFulfilledWithoutCapturedPaymentAmountMinor();

    List<ReconciliationAmount> sumRefundedAmountMinor();
}
