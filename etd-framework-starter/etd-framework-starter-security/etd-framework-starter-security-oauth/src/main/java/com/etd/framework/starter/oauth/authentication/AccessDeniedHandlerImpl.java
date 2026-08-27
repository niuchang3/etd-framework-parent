package com.etd.framework.starter.oauth.authentication;

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

    public AccessDeniedHandlerImpl(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    /**
     * 返回 403 权限不足响应。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param exception 权限异常
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) throws IOException, ServletException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        writeFailed(request, response, exception);
    }
}
