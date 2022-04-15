package com.etd.framework.authorization.password;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class UserPassworAuthenticationProvider implements AuthenticationProvider {


    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        UserPasswordToken userPasswordToken = (UserPasswordToken) authentication;


        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UserPasswordTokenAuthenticationConverter.class.isAssignableFrom(authentication);
    }
}
