package com.unicorn.gameshop.event.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.gameshop.common.IdGenerator;
import com.unicorn.gameshop.event.mapper.OutboxEventMapper;
import com.unicorn.gameshop.event.model.OutboxEvent;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class OutboxEventService {

    private final OutboxEventMapper outboxEventMapper;
    private final ObjectMapper objectMapper;

    public OutboxEventService(OutboxEventMapper outboxEventMapper, ObjectMapper objectMapper) {
        this.outboxEventMapper = outboxEventMapper;
        this.objectMapper = objectMapper;
    }

    public void record(String eventType, String aggregateType, String aggregateId, Object payload) {
        try {
            Instant now = Instant.now();
            OutboxEvent event = new OutboxEvent();
            event.setId(IdGenerator.id());
            event.setEventType(eventType);
            event.setAggregateType(aggregateType);
            event.setAggregateId(aggregateId);
            event.setPayloadJson(objectMapper.writeValueAsString(payload));
            event.setStatus("PENDING");
            event.setAttempts(0);
            event.setCreatedAt(now);
            outboxEventMapper.insert(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize outbox event", exception);
        }
    }

    public List<OutboxEvent> list(String status, int limit) {
        return outboxEventMapper.list(status, Math.min(Math.max(limit, 1), 100));
    }

    public long count(String status) {
        return outboxEventMapper.countByStatus(status);
    }

    public void markRetryable(String id, String reason) {
        outboxEventMapper.markRetryable(id, Instant.now(), reason);
    }
}
