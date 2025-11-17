package com.hkd.auth.application.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT配置属性
 *
 * @author HKD Team
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "hkd.jwt")
public class JwtConfig {

    /**
     * JWT密钥
     */
    private String secret;

    /**
     * Access Token过期时间（秒）
     */
    private Long accessTokenExpire;

    /**
     * Refresh Token过期时间（秒）
     */
    private Long refreshTokenExpire;

    /**
     * Token签发者
     */
    private String issuer;
}
