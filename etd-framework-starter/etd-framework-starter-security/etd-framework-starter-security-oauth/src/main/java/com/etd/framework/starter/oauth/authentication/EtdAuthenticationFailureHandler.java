package com.etd.framework.starter.oauth.authentication;

import org.etd.framework.common.core.model.ResultModel;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;



public class EtdAuthenticationFailureHandler extends AbstractAuthenticationHandler implements AuthenticationFailureHandler {


    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        writeFailed(org.springframework.http.HttpStatus.OK,request,response,exception);
    }
}
