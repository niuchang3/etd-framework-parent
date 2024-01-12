package com.etd.framework.starter.oauth.authentication.converter.extend;

import com.etd.framework.starter.oauth.authentication.constant.Oauth2ParameterConstant;
import com.etd.framework.starter.oauth.authentication.converter.AbstractAuthenticationConverter;
import com.etd.framework.starter.oauth.authentication.token.request.Oauth2AuthorizationTokenAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

/**
 * 授权码Token 认证转换器
 */
public class Oauth2AuthorizeTokenRequestConverter extends AbstractAuthenticationConverter implements AuthenticationConverter {

    @Override
    protected String getGrantType() {
        return Oauth2ParameterConstant.GRANT_TYPE.authorization_code.name();
    }

    @Override
    protected Authentication doConvert(HttpServletRequest request) {
        String grantType = obtainRequestParams(request, Oauth2ParameterConstant.GRANT_TYPE.class.getSimpleName().toLowerCase());
        String code = obtainRequestParams(request, Oauth2ParameterConstant.AuthorizationTokenAuthentication.code.name());
        String redirectUri = obtainRequestParams(request, Oauth2ParameterConstant.AuthorizationTokenAuthentication.redirect_uri.name());
        String clientId = obtainRequestParams(request, Oauth2ParameterConstant.AuthorizationTokenAuthentication.client_id.name());

        Oauth2AuthorizationTokenAuthenticationToken authenticationToken = new Oauth2AuthorizationTokenAuthenticationToken(clientId, Collections.emptyList());
        authenticationToken.setRedirectUri(redirectUri);
        authenticationToken.setGrantType(grantType);
        authenticationToken.setCode(code);
        return authenticationToken;
    }
}
