package com.etd.framework.exception;


import org.etd.framework.common.core.constants.Oauth2ErrorCodeConstant;
import org.etd.framework.common.core.constants.RequestCodeConverter;
import org.etd.framework.common.core.model.ResultModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Young
 * @description 统一异常处理
 * @date 2020/6/23
 */

public class Oauth2AuthenticationFailureHandler implements AuthenticationFailureHandler {


    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        OAuth2Error error = ((OAuth2AuthenticationException) exception).getError();
        ServletServerHttpResponse httpResponse = new ServletServerHttpResponse(response);
        httpResponse.setStatusCode(HttpStatus.BAD_REQUEST);

        final HttpHeaders headers = httpResponse.getHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        MappingJackson2HttpMessageConverter jackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        ResultModel resultModel = RequestCodeConverter.convertError(Oauth2ErrorCodeConstant.values(), error.getErrorCode());
        jackson2HttpMessageConverter.write(resultModel, MediaType.APPLICATION_JSON, httpResponse);
        httpResponse.getBody().flush();
    }

}
