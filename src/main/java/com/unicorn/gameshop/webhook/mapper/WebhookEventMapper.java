package com.unicorn.gameshop.webhook.mapper;

import com.unicorn.gameshop.webhook.model.WebhookEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

@Mapper
public interface WebhookEventMapper {

    int countByProviderEventId(@Param("provider") String provider,
                               @Param("providerEventId") String providerEventId);

    WebhookEvent findByProviderEventId(@Param("provider") String provider,
                                       @Param("providerEventId") String providerEventId);

    List<WebhookEvent> listRecent(@Param("limit") int limit);

    int insert(@Param("id") String id,
               @Param("provider") String provider,
               @Param("providerEventId") String providerEventId,
               @Param("eventType") String eventType,
               @Param("rawBody") String rawBody,
               @Param("signatureVerified") boolean signatureVerified,
               @Param("processStatus") String processStatus,
               @Param("headersJson") String headersJson,
               @Param("receivedAt") Instant receivedAt);

    int markProcessed(@Param("provider") String provider,
                      @Param("providerEventId") String providerEventId,
                      @Param("processedAt") Instant processedAt);
}
