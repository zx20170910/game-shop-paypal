package com.unicorn.gameshop.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.gameshop.common.IdGenerator;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class AuditService {

    private final AuditMapper auditMapper;
    private final ObjectMapper objectMapper;

    public AuditService(AuditMapper auditMapper, ObjectMapper objectMapper) {
        this.auditMapper = auditMapper;
        this.objectMapper = objectMapper;
    }

    public void record(Authentication authentication,
                       String action,
                       String resourceType,
                       String resourceId,
                       Map<String, Object> detail) {
        String operatorId = authentication instanceof JwtAuthenticationToken jwt
                ? jwt.getToken().getClaimAsString("adminId") : authentication.getName();
        String username = authentication.getName();
        String detailJson;
        try {
            detailJson = detail == null ? null : objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            detailJson = "{\"serializationError\":true}";
        }
        auditMapper.insert(IdGenerator.id(), operatorId, username, action, resourceType, resourceId,
                detailJson, Instant.now());
    }
}
