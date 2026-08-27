package com.etd.framework.starter.oauth.authentication.internal.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import com.etd.framework.starter.oauth.authentication.internal.token.UserPasswordAuthenticationRequestToken;

import java.io.IOException;

/**
 * 用户名密码请求转换器。
 * <p>
 * 前后端分离登录接口只接受 JSON 请求体，不从 URL 参数读取账号和密码。
 */
public class UserPasswordRequestAuthenticationConverter implements AuthenticationConverter {

    private final ObjectMapper objectMapper;

    public UserPasswordRequestAuthenticationConverter(ObjectMapper objectMapper) {
        Assert.notNull(objectMapper, "JSON 映射器不能为空。");
        this.objectMapper = objectMapper;
    }

    /**
     * 从 JSON 请求体中读取用户名和密码。
     *
     * @param request 当前请求
     * @return 用户名密码认证请求
     */
    @Override
    public Authentication convert(HttpServletRequest request) {
        UserPasswordLoginRequest loginRequest = readJson(request, UserPasswordLoginRequest.class);
        UserPasswordAuthenticationRequestToken token = new UserPasswordAuthenticationRequestToken(null);
        token.setUsername(loginRequest.getUsername());
        token.setPassword(loginRequest.getPassword());
        return token;
    }

    /**
     * 读取 JSON 请求体。
     *
     * @param request 当前请求
     * @param targetType 目标类型
     * @return 请求体对象
     */
    private <T> T readJson(HttpServletRequest request, Class<T> targetType) {
        if (!isJsonRequest(request)) {
            throw new AuthenticationServiceException("登录接口只支持 JSON 请求体。");
        }
        try {
            return objectMapper.readValue(request.getInputStream(), targetType);
        } catch (IOException exception) {
            throw new AuthenticationServiceException("登录请求体解析失败。", exception);
        }
    }

    /**
     * 判断请求是否为 JSON。
     *
     * @param request 当前请求
     * @return 是否 JSON 请求
     */
    private boolean isJsonRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return StringUtils.hasText(contentType) && contentType.toLowerCase().contains(MediaType.APPLICATION_JSON_VALUE);
    }
}
