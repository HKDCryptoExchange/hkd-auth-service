package com.hkd.auth.application.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * TOTP配置属性
 *
 * @author HKD Team
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "hkd.totp")
public class TotpConfig {

    /**
     * TOTP验证时间窗口大小
     * 允许的时间窗口数量（前后各windowSize个时间窗口）
     */
    private Integer windowSize = 1;

    /**
     * TOTP签发者名称
     * 用于生成QR码时显示的应用名称
     */
    private String issuer = "HKD Exchange";
}
