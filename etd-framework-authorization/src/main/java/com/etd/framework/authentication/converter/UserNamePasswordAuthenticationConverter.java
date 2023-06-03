package com.etd.framework.authentication.converter;

import com.etd.framework.authentication.token.UserNamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;

import javax.servlet.http.HttpServletRequest;

public class UserNamePasswordAuthenticationConverter implements AuthenticationConverter {

    public static final String LOGIN_TYPE = "loginType";


    @Override
    public Authentication convert(HttpServletRequest request) {
        String loginType = obtainLoginType(request);
        if (!LOGIN_TYPE.equals(loginType)) {
            return null;
        }
        return new UserNamePasswordAuthenticationToken("", "", "");
    }

    private String obtainLoginType(HttpServletRequest request) {
        return request.getParameter(LOGIN_TYPE);
    }
}
