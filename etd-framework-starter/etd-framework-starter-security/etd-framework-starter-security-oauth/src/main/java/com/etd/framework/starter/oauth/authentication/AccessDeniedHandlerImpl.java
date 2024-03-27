package com.etd.framework.starter.oauth.authentication;

import cn.hutool.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 权限异常
 */
@Slf4j
public class AccessDeniedHandlerImpl extends AbstractAuthenticationHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) throws IOException, ServletException {
        response.setStatus(HttpStatus.HTTP_FORBIDDEN);
        writeFailed(request, response, exception);
    }
}
