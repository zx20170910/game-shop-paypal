package com.unicorn.gameshop.reconciliation.dto;

import java.util.List;

public record ReconciliationSummary(
        long capturedWithoutFulfillment,
        long fulfilledWithoutCapturedPayment,
        long paymentCaptureWithoutPaidOrder,
        long pendingWebhookEvents,
        long pendingOutboxEvents,
        List<ReconciliationAmount> capturedWithoutFulfillmentAmounts,
        List<ReconciliationAmount> fulfilledWithoutCapturedPaymentAmounts,
        List<ReconciliationAmount> refundedAmounts
) {
}
