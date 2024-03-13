package com.etd.framework.starter.client.core.token;

import lombok.Setter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class BearerTokenAuthentication extends AbstractAuthenticationToken {

    @Setter
    private String credentials;

    @Setter
    private String principal;
    /**
     * Creates a token with the supplied array of authorities.
     *
     * @param authorities the collection of <tt>GrantedAuthority</tt>s for the principal
     *                    represented by this authentication object.
     */
    public BearerTokenAuthentication(Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
    }

    public BearerTokenAuthentication(Collection<? extends GrantedAuthority> authorities, String credentials) {
        super(authorities);
        this.credentials = credentials;
    }

    @Override
    public String getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
