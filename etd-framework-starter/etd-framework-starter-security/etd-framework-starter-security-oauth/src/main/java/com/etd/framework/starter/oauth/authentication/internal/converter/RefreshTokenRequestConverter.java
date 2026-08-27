package com.etd.framework.starter.oauth.authentication.internal.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.etd.framework.starter.client.core.i18n.SecurityMessageCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import com.etd.framework.starter.oauth.authentication.internal.token.RefreshTokenRequestToken;

import java.io.IOException;

/**
 * 刷新令牌请求转换器。
 * <p>
 * 刷新接口只接受 JSON 请求体，不从 URL 参数读取刷新令牌。
 */
public class RefreshTokenRequestConverter implements AuthenticationConverter {

    private final ObjectMapper objectMapper;

    public RefreshTokenRequestConverter(ObjectMapper objectMapper) {
        Assert.notNull(objectMapper, "JSON 映射器不能为空。");
        this.objectMapper = objectMapper;
    }

    /**
     * 转换刷新令牌请求。
     *
     * @param request 当前请求
     * @return 刷新令牌认证对象
     */
    @Override
    public Authentication convert(HttpServletRequest request) {
        RefreshTokenLoginRequest refreshRequest = readJson(request, RefreshTokenLoginRequest.class);
        RefreshTokenRequestToken token = new RefreshTokenRequestToken(null);
        token.setCredentials(refreshRequest.getRefreshToken());
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
            throw new AuthenticationServiceException(SecurityMessageCode.REQUEST_CONTENT_TYPE_INVALID);
        }
        try {
            return objectMapper.readValue(request.getInputStream(), targetType);
        } catch (IOException exception) {
            throw new AuthenticationServiceException(SecurityMessageCode.REQUEST_BODY_PARSE_FAILED, exception);
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
