package com.etd.framework.starter.client.core.token;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 单个令牌值。
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class TokenValue {

    /**
     * 令牌原文。
     */
    private String value;

    /**
     * 令牌过期时间。
     */
    private Instant expires;
}
