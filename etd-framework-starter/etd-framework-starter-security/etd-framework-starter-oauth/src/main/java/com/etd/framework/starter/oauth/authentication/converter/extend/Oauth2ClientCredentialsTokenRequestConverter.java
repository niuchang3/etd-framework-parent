package com.etd.framework.starter.oauth.authentication.converter.extend;

import com.etd.framework.starter.oauth.authentication.constant.Oauth2ParameterConstant;
import com.etd.framework.starter.oauth.authentication.converter.AbstractAuthenticationConverter;
import com.etd.framework.starter.oauth.authentication.token.request.Oauth2ClientCredentialsAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

/**
 * 客户端凭证 认证转换器
 */
public class Oauth2ClientCredentialsTokenRequestConverter extends AbstractAuthenticationConverter implements AuthenticationConverter {

    @Override
    protected String getGrantType() {
        return Oauth2ParameterConstant.GRANT_TYPE.client_credentials.name();
    }

    @Override
    protected Authentication doConvert(HttpServletRequest request) {

        String grantType = obtainRequestParams(request, Oauth2ParameterConstant.GRANT_TYPE.class.getSimpleName().toLowerCase());
        Oauth2ClientCredentialsAuthenticationToken authenticationToken = new Oauth2ClientCredentialsAuthenticationToken(null, Collections.emptyList());
        authenticationToken.setGrantType(grantType);

        return authenticationToken;
    }
}
