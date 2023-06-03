package com.etd.framework.authentication.token;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;


public class UserNamePasswordAuthenticationToken extends UsernamePasswordAuthenticationToken {


    private String captcha;


    public UserNamePasswordAuthenticationToken(Object principal, Object credentials, String captcha) {
        super(principal, credentials);
        this.captcha = captcha;
    }

    public UserNamePasswordAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities, String captcha) {
        super(principal, credentials, authorities);
        this.captcha = captcha;
    }
}
