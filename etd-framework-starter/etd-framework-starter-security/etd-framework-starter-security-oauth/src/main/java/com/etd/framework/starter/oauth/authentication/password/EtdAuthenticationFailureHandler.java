package com.etd.framework.starter.oauth.authentication.password;

import org.etd.framework.common.core.model.ResultModel;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;



public class EtdAuthenticationFailureHandler extends AbstractAuthenticationHandler implements AuthenticationFailureHandler {


    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {

        ServletServerHttpResponse httpResponse = new ServletServerHttpResponse(response);
        httpResponse.setStatusCode(org.springframework.http.HttpStatus.OK);
        ResultModel<Object> failed = ResultModel.failed(org.springframework.http.HttpStatus.UNAUTHORIZED.value(), exception.getCause(), exception.getMessage(), request.getRequestURI());
        getConverter().write(failed, MediaType.APPLICATION_JSON, httpResponse);

    }
}
