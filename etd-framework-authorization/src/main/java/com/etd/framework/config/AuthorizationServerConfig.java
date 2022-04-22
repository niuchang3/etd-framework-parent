package com.etd.framework.config;

import com.etd.framework.authorization.password.UserPasswordAuthenticationProvider;
import com.etd.framework.authorization.password.UserPasswordTokenAuthenticationConverter;
import com.etd.framework.jose.Jwks;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.ClientSettings;
import org.springframework.security.oauth2.server.authorization.config.ProviderSettings;
import org.springframework.security.oauth2.server.authorization.config.TokenSettings;
import org.springframework.security.oauth2.server.authorization.web.OAuth2TokenEndpointFilter;
import org.springframework.security.oauth2.server.authorization.web.authentication.DelegatingAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AuthorizationCodeAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2ClientCredentialsAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2RefreshTokenAuthenticationConverter;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.ObjectUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

@Configuration(proxyBeanMethods = false)
public class AuthorizationServerConfig {

//
//    @Autowired
//    private DataSource dataSource;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
//        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        OAuth2AuthorizationServerConfigurer<HttpSecurity> authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer<>();

        authorizationServerConfigurer.withObjectPostProcessor(new ObjectPostProcessor<OAuth2TokenEndpointFilter>() {
            @Override
            public <O extends OAuth2TokenEndpointFilter> O postProcess(O oauth2TokenEndpointFilter) {
                oauth2TokenEndpointFilter.setAuthenticationConverter(new DelegatingAuthenticationConverter(
                        Arrays.asList(
                                new OAuth2AuthorizationCodeAuthenticationConverter(),
                                new OAuth2RefreshTokenAuthenticationConverter(),
                                new OAuth2ClientCredentialsAuthenticationConverter(),
                                new UserPasswordTokenAuthenticationConverter())));
                return oauth2TokenEndpointFilter;
            }
        });

        RequestMatcher endpointsMatcher = authorizationServerConfigurer
                .getEndpointsMatcher();

        http
                .requestMatcher(endpointsMatcher)
                .authorizeRequests(authorizeRequests -> {
                    authorizeRequests.antMatchers("/oauth2/token").permitAll();
                })
                .authorizeRequests(authorizeRequests ->
                        authorizeRequests.anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers(endpointsMatcher))
                .apply(authorizationServerConfigurer);

        DefaultSecurityFilterChain securityFilterChain = http.formLogin(Customizer.withDefaults()).build();

        UserPasswordAuthenticationProvider userPasswordAuthenticationProvider =
                new UserPasswordAuthenticationProvider(
                        http.getSharedObject(AuthenticationManager.class),
                        http.getSharedObject(JwtEncoder.class),
                        http.getSharedObject(ProviderSettings.class),
                        http.getSharedObject(OAuth2AuthorizationService.class),
                        http.getSharedObject(JwtDecoder.class));

        http.authenticationProvider(userPasswordAuthenticationProvider);

        return securityFilterChain;
    }

    /**
     * 客户端数据库
     *
     * @return
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {

//        Instant issuedAt = Instant.now();
//        Instant expiresAt = issuedAt.plus(8, ChronoUnit.HOURS);


//        ClientSettings tokenSettings = ClientSettings.builder().requireAuthorizationConsent(true).build();

        TokenSettings tokenSettings = TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofHours(2))
                .reuseRefreshTokens(true)
                .refreshTokenTimeToLive(Duration.ofHours(8))
                .idTokenSignatureAlgorithm(SignatureAlgorithm.RS256).build();

        RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("messaging-client")
                .clientSecret("{noop}secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(AuthorizationGrantType.PASSWORD)
                .redirectUri("http://127.0.0.1:8080/login/oauth2/code/messaging-client-oidc")
                .redirectUri("http://127.0.0.1:8080/authorized")
                .scope(OidcScopes.OPENID)
                .scope("message.read")
                .scope("message.write")
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
                .tokenSettings(getTokenSettings())
                .build();


        JdbcRegisteredClientRepository jdbcRegisteredClientRepository = new JdbcRegisteredClientRepository(jdbcTemplate);

        RegisteredClient client = jdbcRegisteredClientRepository.findByClientId(registeredClient.getClientId());
        if (ObjectUtils.isEmpty(client)) {
            jdbcRegisteredClientRepository.save(registeredClient);
        }
        return jdbcRegisteredClientRepository;
    }


    public TokenSettings getTokenSettings() {
        return TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofHours(2))
                .reuseRefreshTokens(true)
                .refreshTokenTimeToLive(Duration.ofHours(8))
                .idTokenSignatureAlgorithm(SignatureAlgorithm.RS256).build();
    }

    /**
     * Oau授权同意服务
     *
     * @return
     */
    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
        // Will be used by the ConsentController
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }


    @Bean
    public OAuth2AuthorizationService authorizationService(JdbcOperations jdbcOperations, RegisteredClientRepository registeredClientRepository) {

        return new JdbcOAuth2AuthorizationService(jdbcOperations, registeredClientRepository);
    }


//
//
//    @Bean
//    public EmbeddedDatabase embeddedDatabase() {
//        // @formatter:off
//        return new EmbeddedDatabaseBuilder()
//                .generateUniqueName(true)
//                .setType(EmbeddedDatabaseType.H2)
//                .setScriptEncoding("UTF-8")
//                .addScript("classpath:oauth2-authorization-schema.sql")
//                .addScript("classpath:oauth2-authorization-consent-schema.sql")
//                .addScript("classpath:oauth2-registered-client-schema.sql")
//                .build();
//        // @formatter:on
//    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = Jwks.generateRsa();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return (jwkSelector, securityContext) -> {

            return jwkSelector.select(jwkSet);
        };
    }

}
