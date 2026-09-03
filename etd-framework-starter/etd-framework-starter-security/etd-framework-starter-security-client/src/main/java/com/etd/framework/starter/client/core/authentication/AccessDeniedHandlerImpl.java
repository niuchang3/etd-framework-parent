package com.etd.framework.starter.client.core.authentication;

import com.etd.framework.starter.client.core.i18n.SecurityMessageCode;
import com.etd.framework.starter.client.core.i18n.SecurityMessageResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 已认证但权限不足的访问拒绝处理器。
 */
@Component
public class AccessDeniedHandlerImpl extends AbstractAuthenticationHandler implements AccessDeniedHandler {

    public AccessDeniedHandlerImpl(ObjectMapper objectMapper, SecurityMessageResolver securityMessageResolver) {
        super(objectMapper, securityMessageResolver);
    }

    /**
     * 返回 403 权限不足响应。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param exception 权限异常
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        // 原生方法授权与请求授权使用同一国际化响应，异常原因保留在 cause 中。
        writeFailed(request, response, new AccessDeniedException(SecurityMessageCode.ACCESS_DENIED, exception));
    }
}
