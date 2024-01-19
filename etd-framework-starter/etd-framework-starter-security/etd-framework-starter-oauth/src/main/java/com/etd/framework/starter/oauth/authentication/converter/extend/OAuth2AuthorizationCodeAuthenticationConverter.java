package com.etd.framework.starter.oauth.authentication.converter.extend;

import com.etd.framework.starter.oauth.authentication.constant.Oauth2ParameterConstant;
import com.etd.framework.starter.oauth.authentication.converter.AbstractAuthenticationConverter;
import com.etd.framework.starter.oauth.authentication.token.Oauth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationConverter;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

/**
 * 授权码Token 认证转换器
 */
public class OAuth2AuthorizationCodeAuthenticationConverter extends AbstractAuthenticationConverter implements AuthenticationConverter {

    @Override
    protected String getGrantType() {
        return Oauth2ParameterConstant.GRANT_TYPE.authorization_code.name();
    }

    @Override
    protected Authentication doConvert(HttpServletRequest request) {

        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();
        String clientId = (String) clientPrincipal.getPrincipal();
        String clientSecret = (String) clientPrincipal.getCredentials();


        String grantType = obtainRequestParams(request, Oauth2ParameterConstant.GRANT_TYPE.class.getSimpleName().toLowerCase());
        String code = obtainRequestParams(request, Oauth2ParameterConstant.AuthorizationCodeAuthentication.code.name());
        String redirectUri = obtainRequestParams(request, Oauth2ParameterConstant.AuthorizationCodeAuthentication.redirect_uri.name());
        Oauth2AuthorizationCodeAuthenticationToken authenticationToken = new Oauth2AuthorizationCodeAuthenticationToken(clientId, clientSecret, Collections.emptyList());
        authenticationToken.setRedirectUri(redirectUri);
        authenticationToken.setGrantType(grantType);
        authenticationToken.setCode(code);
        return authenticationToken;
    }
}
