package com.etd.framework.starter.oauth.authentication.converter.extend;

import com.etd.framework.starter.oauth.authentication.constant.Oauth2ParameterConstant;
import com.etd.framework.starter.oauth.authentication.converter.AbstractAuthenticationConverter;
import com.etd.framework.starter.oauth.authentication.token.request.Oauth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.core.Authentication;
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

        String responseType = obtainRequestParams(request, Oauth2ParameterConstant.AuthorizationCodeAuthentication.response_type.name());
        String clientId = obtainRequestParams(request, Oauth2ParameterConstant.AuthorizationCodeAuthentication.client_id.name());
        String redirectUri = obtainRequestParams(request, Oauth2ParameterConstant.AuthorizationCodeAuthentication.redirect_uri.name());
        String scope = obtainRequestParams(request, Oauth2ParameterConstant.AuthorizationCodeAuthentication.scope.name());
        String state = obtainRequestParams(request, Oauth2ParameterConstant.AuthorizationCodeAuthentication.state.name());

        Oauth2AuthorizationCodeAuthenticationToken authenticationToken = new Oauth2AuthorizationCodeAuthenticationToken(Collections.emptyList(), clientId);
        authenticationToken.setResponseType(responseType);
        authenticationToken.setRedirectUri(redirectUri);
        authenticationToken.setScope(convertScopeSet(scope));
        authenticationToken.setState(state);
        return authenticationToken;
    }
}
