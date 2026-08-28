package com.etd.framework.starter.oauth.authentication.oauth2.session;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * OAuth2授权端点未登录入口。
 */
@RequiredArgsConstructor
public class OAuth2LoginAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final OAuth2LoginRedirectResolver redirectResolver;

    /**
     * 未登录访问OAuth2授权端点时跳转到统一登录页。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param authException 认证异常
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        response.sendRedirect(redirectResolver.buildLoginRedirect(request));
    }
}
