package com.etd.framework.starter.oauth.authentication.oauth.converter;

import com.etd.framework.starter.client.core.constant.Oauth2ParameterConstant;
import com.etd.framework.starter.client.core.token.Oauth2AuthorizationCodeRequestToken;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class Oauth2AuthorizationCodeRequestConverter implements AuthenticationConverter {


    @Override
    public Authentication convert(HttpServletRequest request) {
        String clientId = request.getParameter(Oauth2ParameterConstant.AuthorizationCodeRequestAuthentication.client_id.name());
        String responseType = request.getParameter(Oauth2ParameterConstant.AuthorizationCodeRequestAuthentication.response_type.name());
        String redirectUri = request.getParameter(Oauth2ParameterConstant.AuthorizationCodeRequestAuthentication.redirect_uri.name());
        String state = request.getParameter(Oauth2ParameterConstant.AuthorizationCodeRequestAuthentication.state.name());
        String[] scope = request.getParameterValues(Oauth2ParameterConstant.AuthorizationCodeRequestAuthentication.scope.name());

        Map<String, String[]> customParameters = Maps.newHashMap();
        request.getParameterMap().forEach((key, item) -> {
            boolean params = Oauth2ParameterConstant.AuthorizationCodeRequestAuthentication.isParams(key);
            if (!params) {
                customParameters.put(key, item);
            }
        });
        Oauth2AuthorizationCodeRequestToken requestToken = new Oauth2AuthorizationCodeRequestToken(null);
        requestToken.setClientId(clientId);
        requestToken.setResponseType(responseType);
        requestToken.setRedirectUri(redirectUri);
        requestToken.setState(state);
        if(!ObjectUtils.isEmpty(scope)){
            requestToken.setScope(Sets.newHashSet(scope));
        }
        if(!ObjectUtils.isEmpty(customParameters)){
            requestToken.setCustomParameters(customParameters);
        }
        requestToken.setAuthenticated(false);
        requestToken.setDetails(null);
        return requestToken;
    }
}
