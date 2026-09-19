package com.unicorn.gameshop.admin.service;

import com.unicorn.gameshop.admin.dto.AdminLoginRequest;
import com.unicorn.gameshop.admin.dto.AdminLoginResponse;
import com.unicorn.gameshop.admin.mapper.AdminUserMapper;
import com.unicorn.gameshop.admin.model.AdminUser;
import com.unicorn.gameshop.common.ApiErrorCode;
import com.unicorn.gameshop.common.BusinessException;
import com.unicorn.gameshop.config.JwtProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AdminAuthService {

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public AdminAuthService(AdminUserMapper adminUserMapper,
                            PasswordEncoder passwordEncoder,
                            JwtEncoder jwtEncoder,
                            JwtProperties jwtProperties) {
        this.adminUserMapper = adminUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
    }

    public AdminLoginResponse login(AdminLoginRequest request) {
        AdminUser user = adminUserMapper.findByUsername(request.username().trim());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ApiErrorCode.ADMIN_AUTH_FAILED, "Invalid administrator credentials");
        }
        List<String> roles = user.getRoles() == null ? List.of() : user.getRoles();
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(jwtProperties.expirationSeconds()))
                .subject(user.getUsername())
                .claim("adminId", user.getId())
                .claim("roles", roles)
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AdminLoginResponse(token, jwtProperties.expirationSeconds(), user.getUsername(), roles);
    }
}
