package com.hkd.auth.application.service;

import com.hkd.auth.api.dto.TokenPair;
import com.hkd.auth.application.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * JWT Token Provider
 * 负责JWT Token的生成和验证
 *
 * @author HKD Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtConfig jwtConfig;

    /**
     * 生成Token对（Access Token + Refresh Token）
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param email    邮箱
     * @param roles    角色列表
     * @return Token对
     */
    public TokenPair generateTokenPair(String userId, String username, String email, List<String> roles) {
        String accessToken = generateAccessToken(userId, username, email, roles);
        String refreshToken = generateRefreshToken(userId);

        return TokenPair.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtConfig.getAccessTokenExpire())
                .build();
    }

    /**
     * 生成Access Token
     */
    public String generateAccessToken(String userId, String username, String email, List<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(jwtConfig.getAccessTokenExpire());

        return Jwts.builder()
                .subject(userId)
                .claim("username", username)
                .claim("email", email)
                .claim("roles", roles)
                .claim("type", "access")
                .issuer(jwtConfig.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成Refresh Token
     */
    public String generateRefreshToken(String userId) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(jwtConfig.getRefreshTokenExpire());

        return Jwts.builder()
                .subject(userId)
                .claim("type", "refresh")
                .issuer(jwtConfig.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 验证并解析Token
     *
     * @param token JWT Token
     * @return Claims
     * @throws JwtException Token无效或过期
     */
    public Claims validateAndParseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT Token已过期: {}", e.getMessage());
            throw new JwtException("Token已过期", e);
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的JWT Token: {}", e.getMessage());
            throw new JwtException("不支持的Token格式", e);
        } catch (MalformedJwtException e) {
            log.warn("JWT Token格式错误: {}", e.getMessage());
            throw new JwtException("Token格式错误", e);
        } catch (SecurityException e) {
            log.warn("JWT Token签名验证失败: {}", e.getMessage());
            throw new JwtException("Token签名无效", e);
        } catch (IllegalArgumentException e) {
            log.warn("JWT Token为空: {}", e.getMessage());
            throw new JwtException("Token不能为空", e);
        }
    }

    /**
     * 从Token中提取用户ID
     */
    public String getUserIdFromToken(String token) {
        Claims claims = validateAndParseToken(token);
        return claims.getSubject();
    }

    /**
     * 从Token中提取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = validateAndParseToken(token);
        return claims.get("username", String.class);
    }

    /**
     * 从Token中提取邮箱
     */
    public String getEmailFromToken(String token) {
        Claims claims = validateAndParseToken(token);
        return claims.get("email", String.class);
    }

    /**
     * 从Token中提取角色列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = validateAndParseToken(token);
        return claims.get("roles", List.class);
    }

    /**
     * 检查Token是否为Access Token
     */
    public boolean isAccessToken(String token) {
        Claims claims = validateAndParseToken(token);
        String type = claims.get("type", String.class);
        return "access".equals(type);
    }

    /**
     * 检查Token是否为Refresh Token
     */
    public boolean isRefreshToken(String token) {
        Claims claims = validateAndParseToken(token);
        String type = claims.get("type", String.class);
        return "refresh".equals(type);
    }

    /**
     * 获取Token过期时间
     */
    public Date getExpirationFromToken(String token) {
        Claims claims = validateAndParseToken(token);
        return claims.getExpiration();
    }

    /**
     * 检查Token是否即将过期（小于5分钟）
     */
    public boolean isTokenExpiringSoon(String token) {
        Date expiration = getExpirationFromToken(token);
        long timeLeft = expiration.getTime() - System.currentTimeMillis();
        return timeLeft < 5 * 60 * 1000; // 5分钟
    }

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
