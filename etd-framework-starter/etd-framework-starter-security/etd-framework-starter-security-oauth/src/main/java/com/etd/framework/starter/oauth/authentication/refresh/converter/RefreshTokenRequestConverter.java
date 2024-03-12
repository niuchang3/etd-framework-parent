package com.etd.framework.starter.oauth.authentication.refresh.converter;

import com.etd.framework.starter.client.core.constant.Oauth2ParameterConstant;
import com.etd.framework.starter.client.core.token.RefreshTokenRequestToken;
import com.etd.framework.starter.client.core.token.UserPasswordAuthenticationRequestToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletRequest;

public class RefreshTokenRequestConverter implements AuthenticationConverter {




    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(Oauth2ParameterConstant.GRANT_TYPE);
        String refreshToken = request.getParameter(Oauth2ParameterConstant.TokenType.refresh_token.name());
        RefreshTokenRequestToken token = new RefreshTokenRequestToken(null);
        token.setCredentials(refreshToken);
        token.setGrantType(grantType);
        return token;
    }
}
