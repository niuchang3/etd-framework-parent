package com.etd.framework.starter.client.core.token;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;


public class RefreshTokenRequestToken extends AbstractAuthenticationToken {

    @Getter
    @Setter
    private String grantType;


    @Setter
    private String credentials;

    @Getter
    @Setter
    private String namespace;
    /**
     * Creates a token with the supplied array of authorities.
     *
     * @param authorities the collection of <tt>GrantedAuthority</tt>s for the principal
     *                    represented by this authentication object.
     */
    public RefreshTokenRequestToken(Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return null;
    }

}
