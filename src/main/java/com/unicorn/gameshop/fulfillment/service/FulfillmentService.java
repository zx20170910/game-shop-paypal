package com.unicorn.gameshop.fulfillment.service;

import com.unicorn.gameshop.common.ApiErrorCode;
import com.unicorn.gameshop.common.BusinessException;
import com.unicorn.gameshop.common.IdGenerator;
import com.unicorn.gameshop.event.service.OutboxEventService;
import com.unicorn.gameshop.fulfillment.dto.CompleteFulfillmentRequest;
import com.unicorn.gameshop.fulfillment.dto.ReviewFulfillmentRequest;
import com.unicorn.gameshop.fulfillment.mapper.FulfillmentMapper;
import com.unicorn.gameshop.fulfillment.model.Fulfillment;
import com.unicorn.gameshop.storage.mapper.FulfillmentAttachmentMapper;
import com.unicorn.gameshop.order.mapper.OrderMapper;
import com.unicorn.gameshop.order.model.FulfillmentStatus;
import com.unicorn.gameshop.order.model.Order;
import com.unicorn.gameshop.order.model.OrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class FulfillmentService {

    private final FulfillmentMapper fulfillmentMapper;
    private final OrderMapper orderMapper;
    private final OutboxEventService outboxEventService;
    private final FulfillmentAttachmentMapper attachmentMapper;

    public FulfillmentService(FulfillmentMapper fulfillmentMapper,
                              OrderMapper orderMapper,
                              OutboxEventService outboxEventService,
                              FulfillmentAttachmentMapper attachmentMapper) {
        this.fulfillmentMapper = fulfillmentMapper;
        this.orderMapper = orderMapper;
        this.outboxEventService = outboxEventService;
        this.attachmentMapper = attachmentMapper;
    }

    @Transactional
    public Fulfillment createIfAbsent(Order order) {
        Fulfillment existing = fulfillmentMapper.findByOrderId(order.getId());
        if (existing != null) {
            return existing;
        }
        Instant now = Instant.now();
        Fulfillment fulfillment = new Fulfillment();
        fulfillment.setId(IdGenerator.id());
        fulfillment.setOrderId(order.getId());
        fulfillment.setTargetPlayerUid(order.getPlayerUid());
        fulfillment.setStatus(FulfillmentStatus.MANUAL_PENDING);
        fulfillment.setAttempts(0);
        fulfillment.setCreatedAt(now);
        fulfillment.setUpdatedAt(now);
        fulfillmentMapper.insert(fulfillment);
        return fulfillment;
    }

    public Fulfillment get(String id) {
        Fulfillment fulfillment = fulfillmentMapper.findById(id);
        if (fulfillment == null) {
            throw new BusinessException(ApiErrorCode.FULFILLMENT_NOT_FOUND, "Fulfillment does not exist");
        }
        return fulfillment;
    }

    public List<Fulfillment> list(FulfillmentStatus status, int limit) {
        return fulfillmentMapper.list(status, Math.min(Math.max(limit, 1), 100));
    }

    @Transactional
    public Fulfillment claim(String id, String operatorId) {
        requireOperator(operatorId);
        Fulfillment existing = get(id);
        Order order = orderMapper.findById(existing.getOrderId());
        if (order != null && "MANUAL_REVIEW".equals(order.getRiskStatus())) {
            throw new BusinessException(ApiErrorCode.FULFILLMENT_NOT_ALLOWED,
                    "Order requires risk review before fulfillment");
        }
        Instant now = Instant.now();
        if (fulfillmentMapper.claim(id, operatorId, now, now) != 1) {
            throw new BusinessException(ApiErrorCode.FULFILLMENT_NOT_ALLOWED,
                    "Fulfillment is not available for claiming");
        }
        return get(id);
    }

    @Transactional
    public Fulfillment complete(String id, String operatorId, CompleteFulfillmentRequest request) {
        requireOperator(operatorId);
        get(id);
        boolean confirmedProof = attachmentMapper.listByFulfillmentId(id).stream()
                .anyMatch(attachment -> "CONFIRMED".equals(attachment.getStatus())
                        && request.proofObjectKey().equals(attachment.getObjectKey()));
        if (!confirmedProof) {
            throw new BusinessException(ApiErrorCode.FULFILLMENT_NOT_ALLOWED,
                    "A confirmed fulfillment attachment matching proofObjectKey is required");
        }
        if (fulfillmentMapper.complete(id, operatorId, request.proofObjectKey(), request.deliveryNote(), Instant.now()) != 1) {
            throw new BusinessException(ApiErrorCode.FULFILLMENT_NOT_ALLOWED,
                    "Fulfillment is not owned by the operator or is not claimable");
        }
        return get(id);
    }

    @Transactional
    public Fulfillment requeue(String id, String reason) {
        Fulfillment existing = get(id);
        if (existing.getStatus() != FulfillmentStatus.MANUAL_REVIEW) {
            throw new BusinessException(ApiErrorCode.FULFILLMENT_NOT_ALLOWED,
                    "Only fulfillment in MANUAL_REVIEW can be requeued");
        }
        attachmentMapper.invalidateConfirmed(id);
        if (fulfillmentMapper.requeue(id, reason.trim(), Instant.now()) != 1) {
            throw new BusinessException(ApiErrorCode.FULFILLMENT_NOT_ALLOWED,
                    "Fulfillment could not be requeued");
        }
        return get(id);
    }

    @Transactional
    public Fulfillment review(String id, ReviewFulfillmentRequest request) {
        Fulfillment fulfillment = get(id);
        if (fulfillment.getStatus() != FulfillmentStatus.AWAITING_REVIEW) {
            throw new BusinessException(ApiErrorCode.FULFILLMENT_NOT_ALLOWED,
                    "Fulfillment is not awaiting review");
        }
        String decision = request.decision().trim().toUpperCase();
        FulfillmentStatus targetStatus = switch (decision) {
            case "APPROVE" -> FulfillmentStatus.FULFILLED;
            case "REJECT" -> FulfillmentStatus.MANUAL_REVIEW;
            default -> throw new BusinessException(ApiErrorCode.BAD_REQUEST,
                    "Review decision must be APPROVE or REJECT");
        };
        Instant now = Instant.now();
        if (fulfillmentMapper.review(id, targetStatus, now,
                targetStatus == FulfillmentStatus.FULFILLED ? now : null, now) != 1) {
            throw new BusinessException(ApiErrorCode.FULFILLMENT_NOT_ALLOWED,
                    "Fulfillment review could not be completed");
        }
        if (targetStatus == FulfillmentStatus.FULFILLED) {
            orderMapper.updateFulfillment(fulfillment.getOrderId(), OrderStatus.FULFILLED,
                    FulfillmentStatus.FULFILLED, now);
            outboxEventService.record("FULFILLMENT_FULFILLED", "FULFILLMENT", id,
                    java.util.Map.of("orderId", fulfillment.getOrderId()));
        }
        return get(id);
    }

    @Transactional
    public int reclaimExpired(long timeoutMinutes) {
        Instant now = Instant.now();
        return fulfillmentMapper.reclaimExpired(now.minus(Duration.ofMinutes(timeoutMinutes)), now);
    }

    private void requireOperator(String operatorId) {
        if (operatorId == null || operatorId.isBlank()) {
            throw new BusinessException(ApiErrorCode.BAD_REQUEST, "Authenticated operator identity is required");
        }
    }
}
