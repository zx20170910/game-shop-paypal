package com.unicorn.gameshop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "risk")
public record RiskProperties(long highAmountMinor, long repeatWindowMinutes, int repeatLimit) {
}
