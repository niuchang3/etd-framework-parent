package com.etd.framework.authorization.password;

import com.etd.framework.utils.JwtUtils;
import com.google.common.collect.Maps;
import lombok.Setter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.authorization.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.config.ProviderSettings;
import org.springframework.util.CollectionUtils;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;


/**
 * 用户名密码认证提供者
 */
public class UserPasswordAuthenticationProvider implements AuthenticationProvider {

    private final JwtEncoder jwtEncoder;

    private final JwtDecoder jwtDecoder;

    private final ProviderSettings providerSettings;

    private final OAuth2AuthorizationService authorizationService;

    private final AuthenticationManager authenticationManager;

    private static final StringKeyGenerator DEFAULT_REFRESH_TOKEN_GENERATOR = new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 96);

    private Supplier<String> refreshTokenGenerator = DEFAULT_REFRESH_TOKEN_GENERATOR::generateKey;


    @Setter
    private OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer = (context) -> {
    };

    public UserPasswordAuthenticationProvider(AuthenticationManager authenticationManager, JwtEncoder jwtEncoder, ProviderSettings providerSettings, OAuth2AuthorizationService authorizationService, JwtDecoder jwtDecoder) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.providerSettings = providerSettings;
        this.authorizationService = authorizationService;
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        UserPasswordAuthenticationToken userPasswordAuthenticationToken = (UserPasswordAuthenticationToken) authentication;


        //验证账号密码
        Map<String, Object> additionalParameters = userPasswordAuthenticationToken.getAdditionalParameters();
        String username = (String) additionalParameters.get(OAuth2ParameterNames.USERNAME);
        String password = (String) additionalParameters.get(OAuth2ParameterNames.PASSWORD);

        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(username, password);


        Authentication userNamePasswordAuthenticate = authenticationManager.authenticate(usernamePasswordAuthenticationToken);


        // 生成token信息
        OAuth2ClientAuthenticationToken clientPrincipal = getAuthenticatedClientElseThrowInvalidClient(userPasswordAuthenticationToken);
        RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();


        String issuer = this.providerSettings != null ? this.providerSettings.getIssuer() : null;
        Set<String> scopes = registeredClient.getScopes();
        if (!CollectionUtils.isEmpty(userPasswordAuthenticationToken.getScopes())) {
            scopes = userPasswordAuthenticationToken.getScopes();
        }


        JwtClaimsSet.Builder claimsBuilder = JwtUtils.accessTokenClaims(
                registeredClient, issuer, userNamePasswordAuthenticate.getName(),
                scopes);

        JoseHeader.Builder headersBuilder = JwtUtils.headers();
        JwtEncodingContext context = JwtEncodingContext.with(headersBuilder, claimsBuilder)
                .registeredClient(registeredClient)
                .principal(userNamePasswordAuthenticate)
                .authorizedScopes(scopes)
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.PASSWORD)
                .authorizationGrant(userPasswordAuthenticationToken)
                .build();

        jwtCustomizer.customize(context);

        JoseHeader headers = context.getHeaders().build();
        JwtClaimsSet claims = context.getClaims().build();
        Jwt jwtAccessToken = this.jwtEncoder.encode(headers, claims);


        Set<String> scope = claims.getClaim(OAuth2ParameterNames.SCOPE);

        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                jwtAccessToken.getTokenValue(), jwtAccessToken.getIssuedAt(),
                jwtAccessToken.getExpiresAt(), scope);

        OAuth2RefreshToken refreshToken = null;
        if (registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN)) {
            refreshToken = generateRefreshToken(registeredClient.getTokenSettings().getRefreshTokenTimeToLive());
        }

        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .principalName(userNamePasswordAuthenticate.getName())
                .authorizationGrantType(AuthorizationGrantType.PASSWORD)
                .token(accessToken,
                        (metadata) ->
                                metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, jwtAccessToken.getClaims()))
                .attribute(OAuth2Authorization.AUTHORIZED_SCOPE_ATTRIBUTE_NAME, scope)
                .attribute(Principal.class.getName(), userNamePasswordAuthenticate);

        if (refreshToken != null) {
            authorizationBuilder.refreshToken(refreshToken);
        }
        OAuth2Authorization authorization = authorizationBuilder.build();
        authorizationService.save(authorization);

        return new OAuth2AccessTokenAuthenticationToken(registeredClient, clientPrincipal, accessToken, refreshToken, Maps.newLinkedHashMap());
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


    private OAuth2RefreshToken generateRefreshToken(Duration tokenTimeToLive) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(tokenTimeToLive);
        return new OAuth2RefreshToken(this.refreshTokenGenerator.get(), issuedAt, expiresAt);
    }


}
