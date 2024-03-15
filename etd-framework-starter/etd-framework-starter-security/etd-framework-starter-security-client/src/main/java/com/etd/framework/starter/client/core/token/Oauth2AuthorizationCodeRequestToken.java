package com.etd.framework.starter.client.core.token;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class Oauth2AuthorizationCodeRequestToken extends AbstractAuthenticationToken {

    @Getter
    @Setter
    private String clientId;

    @Getter
    @Setter
    private String responseType;

    @Getter
    @Setter
    private String redirectUri;

    @Getter
    @Setter
    private String state;

    @Getter
    @Setter
    private Set<String> scope;

    @Getter
    @Setter
    private Map<String,String[]> customParameters;

    /**
     * Creates a token with the supplied array of authorities.
     *
     * @param authorities the collection of <tt>GrantedAuthority</tt>s for the principal
     *                    represented by this authentication object.
     */
    public Oauth2AuthorizationCodeRequestToken(Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
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
