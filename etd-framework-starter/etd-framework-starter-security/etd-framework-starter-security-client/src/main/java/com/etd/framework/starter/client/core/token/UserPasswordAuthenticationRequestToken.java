package com.etd.framework.starter.client.core.token;

import lombok.Setter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;


public class UserPasswordAuthenticationRequestToken extends AbstractAuthenticationToken {

    @Setter
    private String username;

    @Setter
    private String password;


    /**
     * Creates a token with the supplied array of authorities.
     * TenantAuthority
     *
     * @param authorities the collection of <tt>GrantedAuthority</tt>s for the principal
     *                    represented by this authentication object.
     */
    public UserPasswordAuthenticationRequestToken(Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
    }

    @Override
    public Object getCredentials() {
        return password;
    }

    @Override
    public Object getPrincipal() {
        return username;
    }
}
