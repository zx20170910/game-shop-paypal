package com.unicorn.gameshop.event.mapper;

import com.unicorn.gameshop.event.model.OutboxEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

@Mapper
public interface OutboxEventMapper {

    int insert(OutboxEvent event);

    List<OutboxEvent> list(@Param("status") String status, @Param("limit") int limit);

    long countByStatus(@Param("status") String status);

    int markRetryable(@Param("id") String id,
                      @Param("nextAttemptAt") Instant nextAttemptAt,
                      @Param("lastError") String lastError);
}
