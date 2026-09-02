package com.etd.framework.starter.client.core.authentication;

import com.etd.framework.starter.client.core.i18n.SecurityMessageCode;
import com.etd.framework.starter.client.core.i18n.SecurityMessageResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 未认证或认证凭证无效的入口处理器。
 */
@Component
public class AuthenticationEntryPointImpl extends AbstractAuthenticationHandler implements AuthenticationEntryPoint {

    public AuthenticationEntryPointImpl(ObjectMapper objectMapper, SecurityMessageResolver securityMessageResolver) {
        super(objectMapper, securityMessageResolver);
    }

    /**
     * 返回 401 未认证响应。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param authException 认证异常
     */
    /**
     * commence
     *
     * @param request 参数 request
     * @param response 参数 response
     * @param authException 参数 authException
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        AuthenticationException ex = authException;
        if (ex == null || ex.getMessage() == null || ex.getMessage().startsWith("Full authentication is required")) {
            ex = new InsufficientAuthenticationException(SecurityMessageCode.UNAUTHORIZED, authException);
        }
        writeFailed(request, response, ex);
    }
}
