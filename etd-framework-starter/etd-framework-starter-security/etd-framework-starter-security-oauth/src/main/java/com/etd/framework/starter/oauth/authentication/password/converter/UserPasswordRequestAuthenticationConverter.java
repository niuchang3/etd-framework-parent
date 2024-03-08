package com.etd.framework.starter.oauth.authentication.password.converter;

import com.etd.framework.starter.client.core.token.UserPasswordAuthenticationRequestToken;
import com.etd.framework.starter.oauth.constant.Oauth2ParameterConstant;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;

import javax.servlet.http.HttpServletRequest;

/**
 * 用户名密码请求转换器
 */
public class UserPasswordRequestAuthenticationConverter implements AuthenticationConverter {


    @Override
    public Authentication convert(HttpServletRequest request) {
        String username = request.getParameter(Oauth2ParameterConstant.UserPasswordAuthentication.username.name());
        String password = request.getParameter(Oauth2ParameterConstant.UserPasswordAuthentication.password.name());
        UserPasswordAuthenticationRequestToken token = new UserPasswordAuthenticationRequestToken(null);
        token.setUsername(username);
        token.setPassword(password);
        return token;
    }
}
