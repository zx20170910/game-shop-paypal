package com.unicorn.gameshop.common;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class IdGenerator {

    private static final DateTimeFormatter ORDER_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneOffset.UTC);

    private IdGenerator() {
    }

    public static String id() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String orderNo(Clock clock) {
        return "ORD-" + ORDER_TIME.format(Instant.now(clock)) + "-" + id().substring(0, 12).toUpperCase();
    }
}
