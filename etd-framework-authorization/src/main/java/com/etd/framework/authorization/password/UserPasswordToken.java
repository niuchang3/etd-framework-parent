package com.etd.framework.authorization.password;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Arrays;

public class UserPasswordToken extends AbstractAuthenticationToken {


    public UserPasswordToken() {
        super(Arrays.asList());
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return null;
    }
}
