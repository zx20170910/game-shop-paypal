package com.unicorn.gameshop.admin.service;

import com.unicorn.gameshop.admin.model.AuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

@Mapper
public interface AuditMapper {

    int insert(@Param("id") String id,
               @Param("operatorId") String operatorId,
               @Param("operatorUsername") String operatorUsername,
               @Param("action") String action,
               @Param("resourceType") String resourceType,
               @Param("resourceId") String resourceId,
               @Param("detailJson") String detailJson,
               @Param("createdAt") Instant createdAt);

    List<AuditLog> listRecent(@Param("limit") int limit);
}
