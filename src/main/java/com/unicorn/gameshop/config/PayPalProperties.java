package com.unicorn.gameshop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "paypal")
public record PayPalProperties(
        String env,
        String baseUrl,
        String clientId,
        String clientSecret,
        String webhookId,
        long tokenCacheSeconds
) {
}
