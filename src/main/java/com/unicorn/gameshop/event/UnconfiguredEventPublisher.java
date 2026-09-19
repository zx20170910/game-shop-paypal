package com.unicorn.gameshop.event;

import com.unicorn.gameshop.event.model.OutboxEvent;
import org.springframework.stereotype.Component;

@Component
public class UnconfiguredEventPublisher implements EventPublisher {

    @Override
    public void publish(OutboxEvent event) {
        throw new IllegalStateException("RocketMQ publisher is not configured");
    }
}
