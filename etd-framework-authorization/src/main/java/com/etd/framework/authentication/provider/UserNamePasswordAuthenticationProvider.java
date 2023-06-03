package com.etd.framework.authentication.provider;

import com.etd.framework.authentication.token.UserNamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class UserNamePasswordAuthenticationProvider implements AuthenticationProvider {


    /**
     * 处理认证逻辑
     *
     * @param authentication
     * @return
     * @throws AuthenticationException
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.isAssignableFrom(UserNamePasswordAuthenticationToken.class);
    }
}
