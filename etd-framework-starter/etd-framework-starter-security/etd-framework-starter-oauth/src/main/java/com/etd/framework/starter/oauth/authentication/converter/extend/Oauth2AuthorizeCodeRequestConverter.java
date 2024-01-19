package com.etd.framework.starter.oauth.authentication.converter.extend;

import com.etd.framework.starter.oauth.authentication.constant.Oauth2ParameterConstant;
import com.etd.framework.starter.oauth.authentication.converter.AbstractAuthenticationConverter;
import com.etd.framework.starter.oauth.authentication.token.Oauth2AuthorizationCodeRequestToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationConverter;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

/**
 * Oauth 2授权码请求转换器
 */
public class Oauth2AuthorizeCodeRequestConverter extends AbstractAuthenticationConverter implements AuthenticationConverter {

    @Override
    protected String getGrantType() {
        return Oauth2ParameterConstant.GRANT_TYPE.authorization_code.name();
    }

    @Override
    protected Authentication doConvert(HttpServletRequest request) {


        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();
        String clientId = (String) clientPrincipal.getPrincipal();
        String clientSecret = (String) clientPrincipal.getCredentials();

        String responseType = obtainRequestParams(request, Oauth2ParameterConstant.AuthorizationCodeRequestAuthentication.response_type.name());
        String redirectUri = obtainRequestParams(request, Oauth2ParameterConstant.AuthorizationCodeRequestAuthentication.redirect_uri.name());
        String scope = obtainRequestParams(request, Oauth2ParameterConstant.AuthorizationCodeRequestAuthentication.scope.name());
        String state = obtainRequestParams(request, Oauth2ParameterConstant.AuthorizationCodeRequestAuthentication.state.name());


        Oauth2AuthorizationCodeRequestToken authenticationToken = new Oauth2AuthorizationCodeRequestToken(clientId,clientSecret,Collections.emptyList());
        authenticationToken.setResponseType(responseType);
        authenticationToken.setRedirectUri(redirectUri);
        authenticationToken.setScope(convertScopeSet(scope));
        authenticationToken.setState(state);
        return authenticationToken;
    }
}
