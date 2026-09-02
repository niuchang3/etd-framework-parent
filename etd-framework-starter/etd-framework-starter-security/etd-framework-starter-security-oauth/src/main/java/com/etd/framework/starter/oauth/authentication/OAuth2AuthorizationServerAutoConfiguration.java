package com.etd.framework.starter.oauth.authentication;

import com.etd.framework.starter.client.core.authentication.AccessDeniedHandlerImpl;
import com.etd.framework.starter.client.core.encrypt.SecurityKeyLoader;
import com.etd.framework.starter.client.core.properties.SecurityProperties;
import com.etd.framework.starter.oauth.authentication.oauth2.client.OAuth2ClientRepository;
import com.etd.framework.starter.oauth.authentication.oauth2.client.OAuth2RegisteredClientRepository;
import com.etd.framework.starter.oauth.authentication.oauth2.properties.OAuth2AuthorizationServerProperties;
import com.etd.framework.starter.oauth.authentication.oauth2.properties.OAuth2SessionProperties;
import com.etd.framework.starter.oauth.authentication.oauth2.session.OAuth2LoginAuthenticationEntryPoint;
import com.etd.framework.starter.oauth.authentication.oauth2.session.OAuth2LoginRedirectResolver;
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
import org.springframework.security.web.context.SecurityContextRepository;
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
@EnableConfigurationProperties({SecurityProperties.class, OAuth2AuthorizationServerProperties.class, OAuth2SessionProperties.class})
public class OAuth2AuthorizationServerAutoConfiguration {

    /**
     * OAuth2授权服务器过滤器链。
     *
     * @param http HTTP安全构建器
     * @param accessDeniedHandler 访问拒绝处理器
     * @param authenticationEntryPoint OAuth2未登录重定向入口
     * @param securityContextRepository 安全上下文仓储
     * @param sessionProperties OAuth2网页登录态配置
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
                                                                            OAuth2LoginAuthenticationEntryPoint authenticationEntryPoint,
                                                                            SecurityContextRepository securityContextRepository,
                                                                            OAuth2SessionProperties sessionProperties) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                OAuth2AuthorizationServerConfigurer.authorizationServer();
        RequestMatcher endpointsMatcher = authorizationServerConfigurer.getEndpointsMatcher();

        http.securityMatcher(endpointsMatcher)
                .with(authorizationServerConfigurer, authorizationServer -> authorizationServer
                        .registeredClientRepository(registeredClientRepository)
                        .authorizationService(authorizationService)
                        .authorizationConsentService(authorizationConsentService)
                        // 仅客户端要求授权确认时，才会跳转到前端授权确认页。
                        .authorizationEndpoint(endpoint -> endpoint.consentPage(sessionProperties.getConsentPage()))
                        .oidc(Customizer.withDefaults()))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers(endpointsMatcher))
                .securityContext(securityContext -> securityContext.securityContextRepository(securityContextRepository))
                .requestCache(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .accessDeniedHandler(accessDeniedHandler)
                        .authenticationEntryPoint(authenticationEntryPoint));

        return http.build();
    }

    /**
     * Spring授权服务器客户端仓储适配器。
     *
     * @param clientRepository OAuth2客户端仓储
     * @return 注册客户端仓储
     */
    /**
     * registered Client Repository
     *
     * @param clientRepository 参数 clientRepository
     * @return 处理结果
     */
    @Bean
    @ConditionalOnBean(OAuth2ClientRepository.class)
    @ConditionalOnMissingBean(RegisteredClientRepository.class)
    public RegisteredClientRepository registeredClientRepository(OAuth2ClientRepository clientRepository) {
        return new OAuth2RegisteredClientRepository(clientRepository);
    }

    /**
     * OAuth2登录回跳地址解析器。
     *
     * @param properties OAuth2网页登录态配置
     * @return 登录回跳地址解析器
     */
    /**
     * oauth Login Redirect Resolver
     *
     * @param properties 参数 properties
     * @return 处理结果
     */
    @Bean
    @ConditionalOnMissingBean(OAuth2LoginRedirectResolver.class)
    public OAuth2LoginRedirectResolver oauth2LoginRedirectResolver(OAuth2SessionProperties properties) {
        return new OAuth2LoginRedirectResolver(properties);
    }

    /**
     * OAuth2授权端点未登录入口。
     *
     * @param redirectResolver 登录回跳地址解析器
     * @return 未登录入口
     */
    /**
     * oauth Login Authentication Entry Point
     *
     * @param redirectResolver 参数 redirectResolver
     * @return 处理结果
     */
    @Bean
    @ConditionalOnMissingBean(OAuth2LoginAuthenticationEntryPoint.class)
    public OAuth2LoginAuthenticationEntryPoint oauth2LoginAuthenticationEntryPoint(OAuth2LoginRedirectResolver redirectResolver) {
        return new OAuth2LoginAuthenticationEntryPoint(redirectResolver);
    }

    /**
     * OAuth2授权服务器配置。
     *
     * @param properties OAuth2授权服务器配置
     * @return 授权服务器配置
     */
    /**
     * authorization Server Settings
     *
     * @param properties 参数 properties
     * @return 处理结果
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
    /**
     * jwk Source
     *
     * @param securityProperties 参数 securityProperties
     * @return 处理结果
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
    /**
     * jwt Decoder
     *
     * @param jwkSource 参数 jwkSource
     * @return 处理结果
     */
    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }
}
