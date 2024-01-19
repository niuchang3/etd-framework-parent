package com.etd.framework.starter.oauth.authentication.converter.extend;

import com.etd.framework.starter.oauth.authentication.constant.Oauth2ParameterConstant;
import com.etd.framework.starter.oauth.authentication.converter.AbstractAuthenticationConverter;
import com.etd.framework.starter.oauth.authentication.token.Oauth2PasswordAuthenticationToken;
import com.google.common.collect.Lists;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationConverter;

import javax.servlet.http.HttpServletRequest;

/**
 * 用户名密码身份转换器
 * Oauth2.1忠不
 */
@Deprecated
public class Oauth2PasswordAuthenticationConverter extends AbstractAuthenticationConverter implements AuthenticationConverter {


    @Override
    protected String getGrantType() {
        return Oauth2ParameterConstant.GRANT_TYPE.password.name();
    }


    @Override
    protected Authentication doConvert(HttpServletRequest request) {
        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();
        String clientId = (String) clientPrincipal.getPrincipal();
        String clientSecret = (String) clientPrincipal.getCredentials();


        String grantType = obtainRequestParams(request, Oauth2ParameterConstant.GRANT_TYPE.class.getSimpleName().toLowerCase());
        String username = obtainRequestParams(request, Oauth2ParameterConstant.PasswordAuthentication.username.name());
        String password = obtainRequestParams(request, Oauth2ParameterConstant.PasswordAuthentication.password.name());
        String scope = obtainRequestParams(request, Oauth2ParameterConstant.PasswordAuthentication.scope.name());
        String captcha = obtainRequestParams(request, Oauth2ParameterConstant.PasswordAuthentication.captcha.name());

        Oauth2PasswordAuthenticationToken oauth2PasswordAuthenticationToken = new Oauth2PasswordAuthenticationToken(clientId, clientSecret, Lists.newArrayList());
        oauth2PasswordAuthenticationToken.setUsername(username);
        oauth2PasswordAuthenticationToken.setPassword(password);
        oauth2PasswordAuthenticationToken.setGrantType(grantType);
        oauth2PasswordAuthenticationToken.setScope(convertScopeSet(scope));
        oauth2PasswordAuthenticationToken.setCaptcha(captcha);
        return oauth2PasswordAuthenticationToken;
    }
}
