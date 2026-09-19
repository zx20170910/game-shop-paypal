package com.unicorn.gameshop.admin.dto;

import java.util.List;

public record AdminLoginResponse(String accessToken, long expiresInSeconds, String username, List<String> roles) {
}
