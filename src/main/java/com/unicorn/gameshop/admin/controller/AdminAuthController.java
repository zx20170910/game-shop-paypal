package com.unicorn.gameshop.admin.controller;

import com.unicorn.gameshop.admin.dto.AdminLoginRequest;
import com.unicorn.gameshop.admin.dto.AdminLoginResponse;
import com.unicorn.gameshop.admin.service.AdminAuthService;
import com.unicorn.gameshop.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public ApiResponse<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return new ApiResponse<>(adminAuthService.login(request));
    }
}
