package com.hkd.auth.controller;

import com.hkd.auth.api.dto.TokenPair;
import com.hkd.auth.application.service.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器 - 用于生成测试 Token
 * 仅用于开发和测试环境
 *
 * @author HKD Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 临时测试端点：生成测试 Token
     *
     * @param userId 用户ID
     * @param username 用户名
     * @param email 邮箱
     * @return Token 信息
     */
    @GetMapping("/generate-token")
    public Map<String, Object> generateTestToken(
            @RequestParam(defaultValue = "test_user_001") String userId,
            @RequestParam(defaultValue = "testuser") String username,
            @RequestParam(defaultValue = "test@example.com") String email) {

        TokenPair tokenPair = jwtTokenProvider.generateTokenPair(
                userId,
                username,
                email,
                Arrays.asList("USER", "TRADER")
        );

        Map<String, Object> result = new HashMap<>();
        result.put("access_token", tokenPair.getAccessToken());
        result.put("refresh_token", tokenPair.getRefreshToken());
        result.put("token_type", tokenPair.getTokenType());
        result.put("expires_in", tokenPair.getExpiresIn());
        result.put("user_id", userId);
        result.put("username", username);
        result.put("email", email);

        return result;
    }

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "auth-service");
        result.put("message", "Service is running");
        return result;
    }
}
