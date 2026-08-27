package com.etd.framework.starter.oauth.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 未认证或认证凭证无效的入口处理器。
 */
public class AuthenticationEntryPointImpl extends AbstractAuthenticationHandler implements AuthenticationEntryPoint {

    public AuthenticationEntryPointImpl(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    /**
     * 返回 401 未认证响应。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param authException 认证异常
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        writeFailed(request, response, authException);
    }
}
