package com.etd.framework.authorization.password;

import com.etd.framework.utils.JwtUtils;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jwt.JoseHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.server.authorization.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.config.ProviderSettings;


/**
 * 用户名密码认证提供者
 */
public class UserPasswordAuthenticationProvider implements AuthenticationProvider {

    private final JwtEncoder jwtEncoder;

    private ProviderSettings providerSettings;

    public UserPasswordAuthenticationProvider(JwtEncoder jwtEncoder, ProviderSettings providerSettings) {
        this.jwtEncoder = jwtEncoder;
        this.providerSettings = providerSettings;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        UserPasswordAuthenticationToken userPasswordAuthenticationToken = (UserPasswordAuthenticationToken) authentication;
        OAuth2ClientAuthenticationToken clientPrincipal = getAuthenticatedClientElseThrowInvalidClient(userPasswordAuthenticationToken);
        RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();


        JoseHeader.Builder headersBuilder = JwtUtils.headers();
        String issuer = this.providerSettings != null ? this.providerSettings.getIssuer() : null;

        JwtClaimsSet.Builder claimsBuilder = JwtUtils.accessTokenClaims(
                registeredClient, issuer, userPasswordAuthenticationToken.getName(),
                userPasswordAuthenticationToken.getScopes());

//        // @formatter:off
//        JwtEncodingContext context = JwtEncodingContext.with(headersBuilder, claimsBuilder)
//                .registeredClient(registeredClient)
//                .principal(authorization.getAttribute(Principal.class.getName()))
//                .authorization(authorization)
//                .authorizedScopes(authorizedScopes)
//                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
//                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
//                .authorizationGrant(authorizationCodeAuthentication)
//                .build();
//        // @formatter:on
//
//        this.jwtCustomizer.customize(context);
//
//        JoseHeader headers = context.getHeaders().build();
//        JwtClaimsSet claims = context.getClaims().build();
//        Jwt jwtAccessToken = this.jwtEncoder.encode(headers, claims);


//        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
//                jwtAccessToken.getTokenValue(), jwtAccessToken.getIssuedAt(),
//                jwtAccessToken.getExpiresAt(), authorizedScopes);
//
//        new OAuth2AccessTokenAuthenticationToken(clientPrincipal.getRegisteredClient(), clientPrincipal, );
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UserPasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }


    private OAuth2ClientAuthenticationToken getAuthenticatedClientElseThrowInvalidClient(Authentication authentication) {

        OAuth2ClientAuthenticationToken clientPrincipal = null;

        if (OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication.getPrincipal().getClass())) {
            clientPrincipal = (OAuth2ClientAuthenticationToken) authentication.getPrincipal();
        }

        if (clientPrincipal != null && clientPrincipal.isAuthenticated()) {
            return clientPrincipal;
        }

        throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
    }
}
