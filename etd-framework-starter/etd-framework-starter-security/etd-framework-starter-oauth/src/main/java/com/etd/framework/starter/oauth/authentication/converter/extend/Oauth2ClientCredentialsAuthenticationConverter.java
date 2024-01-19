package com.etd.framework.starter.oauth.authentication.converter.extend;

import com.etd.framework.starter.oauth.authentication.constant.Oauth2ParameterConstant;
import com.etd.framework.starter.oauth.authentication.converter.AbstractAuthenticationConverter;
import com.etd.framework.starter.oauth.authentication.token.Oauth2ClientCredentialsAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationConverter;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

/**
 * 客户端凭证 认证转换器
 */
public class Oauth2ClientCredentialsAuthenticationConverter extends AbstractAuthenticationConverter implements AuthenticationConverter {

    @Override
    protected String getGrantType() {
        return Oauth2ParameterConstant.GRANT_TYPE.client_credentials.name();
    }

    @Override
    protected Authentication doConvert(HttpServletRequest request) {
        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();
        String clientId = (String) clientPrincipal.getPrincipal();
        String clientSecret = (String) clientPrincipal.getCredentials();


        String grantType = obtainRequestParams(request, Oauth2ParameterConstant.GRANT_TYPE.class.getSimpleName().toLowerCase());
        String scope = obtainRequestParams(request, Oauth2ParameterConstant.ClientCredentialsAuthentication.scope.name());
        Oauth2ClientCredentialsAuthenticationToken authenticationToken = new Oauth2ClientCredentialsAuthenticationToken(clientId, clientSecret, Collections.emptyList());
        authenticationToken.setGrantType(grantType);
        authenticationToken.setScope(convertScopeSet(scope));
        return authenticationToken;
    }
}
