package com.etd.framework.starter.client.core.authentication;

import com.etd.framework.starter.client.core.i18n.SecurityMessageResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 认证失败响应处理器。
 * <p>
 * 统一把 Spring Security 的认证异常转换为框架响应体。
 */
@Component
public class EtdAuthenticationFailureHandler extends AbstractAuthenticationHandler implements AuthenticationFailureHandler {

    public EtdAuthenticationFailureHandler(ObjectMapper objectMapper, SecurityMessageResolver securityMessageResolver) {
        super(objectMapper, securityMessageResolver);
    }

    /**
     * 写出认证失败响应。
     *
     * @param request   当前请求
     * @param response  当前响应
     * @param exception 认证异常
     */
    /**
     * on Authentication Failure
     *
     * @param request 参数 request
     * @param response 参数 response
     * @param exception 参数 exception
     */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        if (response.getStatus() < HttpStatus.BAD_REQUEST.value()) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
        writeFailed(request, response, exception);
    }
}
