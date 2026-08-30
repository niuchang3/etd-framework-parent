package com.etd.framework.starter.oauth.authentication.internal.converter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.etd.framework.starter.client.core.constant.SecurityParameterConstant;
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
    @JsonProperty(SecurityParameterConstant.REFRESH_TOKEN_PARAMETER)
    private String refreshToken;
}
