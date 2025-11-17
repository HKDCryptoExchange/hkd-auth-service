package com.hkd.auth.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Token对（Access Token + Refresh Token）
 *
 * @author HKD Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenPair implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Access Token
     */
    private String accessToken;

    /**
     * Refresh Token
     */
    private String refreshToken;

    /**
     * Token类型（通常为"Bearer"）
     */
    private String tokenType;

    /**
     * Access Token过期时间（秒）
     */
    private Long expiresIn;
}
