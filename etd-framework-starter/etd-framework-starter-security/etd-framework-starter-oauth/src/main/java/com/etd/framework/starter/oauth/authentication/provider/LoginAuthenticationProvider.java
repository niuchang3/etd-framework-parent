package com.etd.framework.starter.oauth.authentication.provider;

import com.etd.framework.starter.oauth.authentication.entity.Oauth2UserDetails;
import com.etd.framework.starter.oauth.authentication.service.UserPasswordService;
import com.etd.framework.starter.oauth.authentication.token.LoginAuthenticationToken;
import com.etd.framework.starter.oauth.authentication.token.OAuth2AccessToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class LoginAuthenticationProvider implements AuthenticationProvider {


    private UserPasswordService userPasswordService;


    public LoginAuthenticationProvider(UserPasswordService userPasswordService) {
        this.userPasswordService = userPasswordService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        //TODO 假逻辑
        Oauth2UserDetails details = userPasswordService.loadUserByName((String) authentication.getPrincipal());
        Object principal = authentication.getPrincipal();
        Object credentials = authentication.getCredentials();

        OAuth2AccessToken token = new OAuth2AccessToken(details.getAuthorities(),principal,credentials);
        token.setAuthenticated(true);
        return token;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return LoginAuthenticationToken.class.isAssignableFrom(authentication);
    }


}
