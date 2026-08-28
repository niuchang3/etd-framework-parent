package com.etd.framework.starter.oauth.authentication;

import com.etd.framework.starter.client.core.authentication.AccessDeniedHandlerImpl;
import com.etd.framework.starter.client.core.authentication.AuthenticationEntryPointImpl;
import com.etd.framework.starter.client.core.encrypt.SecurityKeyLoader;
import com.etd.framework.starter.client.core.properties.SecurityProperties;
import com.etd.framework.starter.oauth.authentication.oauth2.client.OAuth2ClientRepository;
import com.etd.framework.starter.oauth.authentication.oauth2.client.OAuth2RegisteredClientRepository;
import com.etd.framework.starter.oauth.authentication.oauth2.properties.OAuth2AuthorizationServerProperties;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;

import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * 标准OAuth2授权服务器自动配置。
 */
@AutoConfiguration
@ConditionalOnClass(OAuth2AuthorizationServerConfigurer.class)
@ConditionalOnProperty(prefix = "system.security.oauth2.authorization-server", name = "enabled", havingValue = "true")
@EnableConfigurationProperties({SecurityProperties.class, OAuth2AuthorizationServerProperties.class})
public class OAuth2AuthorizationServerAutoConfiguration {

    /**
     * OAuth2授权服务器过滤器链。
     *
     * @param http HTTP安全构建器
     * @param accessDeniedHandler 访问拒绝处理器
     * @param authenticationEntryPoint 认证入口处理器
     * @return OAuth2授权服务器过滤器链
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    @ConditionalOnBean({
            RegisteredClientRepository.class,
            OAuth2AuthorizationService.class,
            OAuth2AuthorizationConsentService.class
    })
    @ConditionalOnMissingBean(name = "oauth2AuthorizationServerSecurityFilterChain")
    public SecurityFilterChain oauth2AuthorizationServerSecurityFilterChain(HttpSecurity http,
                                                                            RegisteredClientRepository registeredClientRepository,
                                                                            OAuth2AuthorizationService authorizationService,
                                                                            OAuth2AuthorizationConsentService authorizationConsentService,
                                                                            AccessDeniedHandlerImpl accessDeniedHandler,
                                                                            AuthenticationEntryPointImpl authenticationEntryPoint) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                OAuth2AuthorizationServerConfigurer.authorizationServer();
        RequestMatcher endpointsMatcher = authorizationServerConfigurer.getEndpointsMatcher();

        http.securityMatcher(endpointsMatcher)
                .with(authorizationServerConfigurer, authorizationServer -> authorizationServer
                        .registeredClientRepository(registeredClientRepository)
                        .authorizationService(authorizationService)
                        .authorizationConsentService(authorizationConsentService)
                        .oidc(Customizer.withDefaults()))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers(endpointsMatcher))
                .requestCache(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .accessDeniedHandler(accessDeniedHandler)
                        .authenticationEntryPoint(authenticationEntryPoint))
                .formLogin(Customizer.withDefaults());

        return http.build();
    }

    /**
     * Spring授权服务器客户端仓储适配器。
     *
     * @param clientRepository OAuth2客户端仓储
     * @return 注册客户端仓储
     */
    @Bean
    @ConditionalOnBean(OAuth2ClientRepository.class)
    @ConditionalOnMissingBean(RegisteredClientRepository.class)
    public RegisteredClientRepository registeredClientRepository(OAuth2ClientRepository clientRepository) {
        return new OAuth2RegisteredClientRepository(clientRepository);
    }

    /**
     * OAuth2授权服务器配置。
     *
     * @param properties OAuth2授权服务器配置
     * @return 授权服务器配置
     */
    @Bean
    @ConditionalOnMissingBean(AuthorizationServerSettings.class)
    public AuthorizationServerSettings authorizationServerSettings(OAuth2AuthorizationServerProperties properties) {
        AuthorizationServerSettings.Builder builder = AuthorizationServerSettings.builder();
        if (StringUtils.hasText(properties.getIssuer())) {
            builder.issuer(properties.getIssuer());
        }
        return builder.build();
    }

    /**
     * OAuth2授权服务器JWK密钥源。
     *
     * @param securityProperties 安全配置
     * @return JWK密钥源
     */
    @Bean
    @ConditionalOnMissingBean(JWKSource.class)
    public JWKSource<SecurityContext> jwkSource(SecurityProperties securityProperties) {
        RSAPublicKey publicKey = SecurityKeyLoader.loadPublicKey(securityProperties);
        PrivateKey privateKey = SecurityKeyLoader.loadPrivateKey(securityProperties);
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey((RSAPrivateKey) privateKey)
                .build();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    /**
     * JWT解码器，用于OIDC UserInfo等端点校验令牌。
     *
     * @param jwkSource JWK密钥源
     * @return JWT解码器
     */
    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }
}
