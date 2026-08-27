package com.etd.framework.starter.oauth.authentication.internal.converter;

import lombok.Getter;
import lombok.Setter;

/**
 * 用户名密码登录 JSON 请求体。
 */
@Getter
@Setter
public class UserPasswordLoginRequest {

    /**
     * 登录账号。
     */
    private String username;

    /**
     * 登录密码。
     */
    private String password;
}
