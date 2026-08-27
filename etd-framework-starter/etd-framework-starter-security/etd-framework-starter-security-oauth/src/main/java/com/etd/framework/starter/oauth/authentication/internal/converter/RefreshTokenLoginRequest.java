package com.etd.framework.starter.oauth.authentication.internal.converter;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 刷新令牌 JSON 请求体。
 */
@Getter
@Setter
public class RefreshTokenLoginRequest {

    /**
     * 刷新令牌。
     */
    @JsonProperty("refresh_token")
    private String refreshToken;
}
