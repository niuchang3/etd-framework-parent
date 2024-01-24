package com.etd.framework.starter.oauth.authentication.provider;

import com.etd.framework.starter.oauth.authentication.token.Oauth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class OAuth2AuthorizationCodeAuthenticationProvider implements AuthenticationProvider {

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return Oauth2AuthorizationCodeAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
