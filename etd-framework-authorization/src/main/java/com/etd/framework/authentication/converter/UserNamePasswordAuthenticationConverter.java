package com.etd.framework.authentication.converter;

import com.etd.framework.authentication.token.UserNamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;

import javax.servlet.http.HttpServletRequest;

public class UserNamePasswordAuthenticationConverter implements AuthenticationConverter {

    private static final String CAPTCHA = "captcha";

    @Override
    public Authentication convert(HttpServletRequest request) {
        String loginType = obtainLoginType(request);
        if (!OAuth2ParameterNames.PASSWORD.equals(loginType)) {
            return null;
        }
        String userName = obtainUserName(request);
        String password = obtainPassword(request);
        String captcha = obtainCaptcha(request);
        return new UserNamePasswordAuthenticationToken(userName, password, captcha);
    }

    private String obtainLoginType(HttpServletRequest request) {
        return request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
    }

    private String obtainUserName(HttpServletRequest request) {
        return request.getParameter(OAuth2ParameterNames.USERNAME);
    }

    private String obtainPassword(HttpServletRequest request) {
        return request.getParameter(OAuth2ParameterNames.PASSWORD);
    }

    private String obtainCaptcha(HttpServletRequest request) {
        return request.getParameter(CAPTCHA);
    }
}
