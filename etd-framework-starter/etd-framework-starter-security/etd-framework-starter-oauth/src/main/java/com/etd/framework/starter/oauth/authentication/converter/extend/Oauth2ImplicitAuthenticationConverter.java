package com.etd.framework.starter.oauth.authentication.converter.extend;

import com.etd.framework.starter.oauth.authentication.constant.Oauth2ParameterConstant;
import com.etd.framework.starter.oauth.authentication.converter.AbstractAuthenticationConverter;
import com.etd.framework.starter.oauth.authentication.token.Oauth2ImplicitAuthenticationToken;
import com.google.common.collect.Lists;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;

import javax.servlet.http.HttpServletRequest;

/**
 * Oauth 2授权隐式请求转换器
 */
@Deprecated
public class Oauth2ImplicitAuthenticationConverter extends AbstractAuthenticationConverter implements AuthenticationConverter {


    @Override
    protected String getGrantType() {
        return Oauth2ParameterConstant.GRANT_TYPE.implicit.name();
    }

    @Override
    protected Authentication doConvert(HttpServletRequest request) {
        String responseType = obtainRequestParams(request, Oauth2ParameterConstant.ImplicitAuthentication.response_type.name());
        String clientId = obtainRequestParams(request, Oauth2ParameterConstant.ImplicitAuthentication.client_id.name());
        String redirectUri = obtainRequestParams(request, Oauth2ParameterConstant.ImplicitAuthentication.redirect_uri.name());
        String scope = obtainRequestParams(request, Oauth2ParameterConstant.ImplicitAuthentication.scope.name());
        String state = obtainRequestParams(request, Oauth2ParameterConstant.ImplicitAuthentication.state.name());

        Oauth2ImplicitAuthenticationToken authenticationToken = new Oauth2ImplicitAuthenticationToken(clientId, Lists.newArrayList());
        authenticationToken.setResponseType(responseType);
        authenticationToken.setRedirectUri(redirectUri);
        authenticationToken.setScope(convertScopeSet(scope));
        authenticationToken.setState(state);
        return authenticationToken;
    }
}
