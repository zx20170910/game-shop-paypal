package com.unicorn.gameshop.event;

import com.unicorn.gameshop.event.model.OutboxEvent;

public interface EventPublisher {

    void publish(OutboxEvent event);
}
