package com.hkd.auth.application.service;

import com.hkd.auth.application.config.TotpConfig;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * TOTP (Time-based One-Time Password) Service
 * 负责TOTP双因素认证的生成和验证
 * 基于RFC 6238标准
 *
 * @author HKD Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class TotpService {

    private final TotpConfig totpConfig;
    private final GoogleAuthenticator googleAuthenticator;

    public TotpService(TotpConfig totpConfig) {
        this.totpConfig = totpConfig;

        // 配置Google Authenticator
        GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder configBuilder =
                new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                        .setTimeStepSizeInMillis(TimeUnit.SECONDS.toMillis(30)) // 30秒时间窗口
                        .setWindowSize(totpConfig.getWindowSize()) // 允许的时间窗口
                        .setCodeDigits(6) // 6位验证码
                        .setKeyRepresentation(com.warrenstrange.googleauth.KeyRepresentation.BASE32);

        this.googleAuthenticator = new GoogleAuthenticator(configBuilder.build());
    }

    /**
     * 生成新的TOTP密钥
     * 用于新用户启用2FA时
     *
     * @return TOTP密钥（Base32编码）
     */
    public String generateSecret() {
        GoogleAuthenticatorKey credentials = googleAuthenticator.createCredentials();
        String secret = credentials.getKey();
        log.debug("生成新的TOTP密钥");
        return secret;
    }

    /**
     * 验证TOTP验证码
     *
     * @param secret TOTP密钥（Base32编码）
     * @param code   用户输入的6位验证码
     * @return 验证是否通过
     */
    public boolean validateCode(String secret, int code) {
        if (secret == null || secret.isEmpty()) {
            log.warn("TOTP密钥为空，验证失败");
            return false;
        }

        try {
            boolean isValid = googleAuthenticator.authorize(secret, code);
            if (isValid) {
                log.debug("TOTP验证成功");
            } else {
                log.debug("TOTP验证失败");
            }
            return isValid;
        } catch (Exception e) {
            log.error("TOTP验证异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证TOTP验证码（字符串格式）
     *
     * @param secret TOTP密钥（Base32编码）
     * @param code   用户输入的6位验证码（字符串）
     * @return 验证是否通过
     */
    public boolean validateCode(String secret, String code) {
        if (code == null || code.isEmpty()) {
            log.warn("TOTP验证码为空");
            return false;
        }

        try {
            int codeInt = Integer.parseInt(code);
            return validateCode(secret, codeInt);
        } catch (NumberFormatException e) {
            log.warn("TOTP验证码格式错误: {}", code);
            return false;
        }
    }

    /**
     * 生成QR码URL
     * 用于用户通过Google Authenticator等应用扫码添加
     *
     * @param username 用户名
     * @param secret   TOTP密钥
     * @return QR码URL（otpauth://格式）
     */
    public String generateQrCodeUrl(String username, String secret) {
        String issuer = totpConfig.getIssuer();
        String qrCodeUrl = GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(
                issuer,
                username,
                googleAuthenticator.createCredentials(secret)
        );
        log.debug("生成QR码URL: username={}, issuer={}", username, issuer);
        return qrCodeUrl;
    }

    /**
     * 生成完整的QR码图片URL
     * 使用Google Charts API生成QR码图片
     *
     * @param username 用户名
     * @param secret   TOTP密钥
     * @return Google Charts API的QR码图片URL
     */
    public String generateQrCodeImageUrl(String username, String secret) {
        String otpAuthUrl = generateQrCodeUrl(username, secret);

        // 使用Google Charts API生成QR码
        // 格式: https://chart.googleapis.com/chart?chs=200x200&cht=qr&chl=<otpauth_url>
        String qrImageUrl = String.format(
                "https://chart.googleapis.com/chart?chs=200x200&chld=M%%7C0&cht=qr&chl=%s",
                urlEncode(otpAuthUrl)
        );

        return qrImageUrl;
    }

    /**
     * 验证密钥格式是否正确
     *
     * @param secret TOTP密钥
     * @return 密钥是否有效
     */
    public boolean isValidSecret(String secret) {
        if (secret == null || secret.isEmpty()) {
            return false;
        }

        try {
            // 尝试生成当前时间的验证码，如果密钥格式正确则不会抛出异常
            googleAuthenticator.getTotpPassword(secret);
            return true;
        } catch (Exception e) {
            log.warn("无效的TOTP密钥格式: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取当前时间的TOTP验证码
     * 主要用于测试和调试
     *
     * @param secret TOTP密钥
     * @return 当前时间窗口的验证码
     */
    public int getCurrentCode(String secret) {
        return googleAuthenticator.getTotpPassword(secret);
    }

    /**
     * URL编码
     */
    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            log.error("URL编码失败: {}", e.getMessage());
            return value;
        }
    }
}
