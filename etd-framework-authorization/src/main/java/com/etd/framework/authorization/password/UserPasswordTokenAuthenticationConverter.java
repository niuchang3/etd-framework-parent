package com.etd.framework.authorization.password;

import com.etd.framework.utils.OAuth2EndpointUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.MultiValueMap;

import javax.servlet.http.HttpServletRequest;

public class UserPasswordTokenAuthenticationConverter implements AuthenticationConverter {


    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!"POST".equals(request.getMethod()) && !AuthorizationGrantType.PASSWORD.equals(grantType)) {
            return null;
        }
        MultiValueMap<String, String> parameters = OAuth2EndpointUtils.getParameters(request);

        return new UserPasswordToken();
    }
}
