package com.etd.framework.starter.oauth;


import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.crypto.PemUtil;
import com.etd.framework.starter.client.core.Oauth2AuthenticationConfigurer;
import com.etd.framework.starter.client.core.configurer.BearerAuthenticationConfigurer;
import com.etd.framework.starter.client.core.encrypt.TokenEncoder;
import com.etd.framework.starter.client.core.encrypt.impl.JwtTokeEncoder;
import com.etd.framework.starter.client.core.oauth.OauthClientService;
import com.etd.framework.starter.client.core.oauth.memory.OauthClientServiceImpl;
import com.etd.framework.starter.client.core.properties.SystemOauthProperties;
import com.etd.framework.starter.client.core.user.IUserService;
import com.etd.framework.starter.client.core.user.PermissionsService;
import com.etd.framework.starter.client.core.user.memory.MemoryPermissionsServiceImpl;
import com.etd.framework.starter.client.core.user.memory.MemoryUserServiceImpl;
import com.etd.framework.starter.oauth.authentication.AccessDeniedHandlerImpl;
import com.etd.framework.starter.oauth.authentication.AuthenticationEntryPointImpl;
import com.etd.framework.starter.oauth.authentication.oauth.configurer.Oauth2AuthorizationCodeConfigurer;
import com.etd.framework.starter.oauth.authentication.password.configurer.UserPasswordAuthenticationConfigurer;
import com.etd.framework.starter.oauth.authentication.refresh.configurer.RefreshTokenAuthenticationConfigurer;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.List;

@Configuration
@ComponentScan({"com.etd.framework.starter.oauth.*"})
@EnableConfigurationProperties
public class OauthAuthenticationConfiguring {

    @Autowired
    private SystemOauthProperties systemOauthProperties;


    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain defaultAuthenticationServer(HttpSecurity http) throws Exception {
        Oauth2AuthenticationConfigurer configurer = new Oauth2AuthenticationConfigurer();
        configurer.addConfigurer(new UserPasswordAuthenticationConfigurer());
        configurer.addConfigurer(new RefreshTokenAuthenticationConfigurer());
        configurer.addConfigurer(new BearerAuthenticationConfigurer());
        configurer.addConfigurer(new Oauth2AuthorizationCodeConfigurer());

        List<String> ignorePermissions = systemOauthProperties.getPermissions().getIgnore();
        String[] urls = ArrayUtil.toArray(CollectionUtil.isEmpty(ignorePermissions) ? Lists.newArrayList():ignorePermissions, String.class);
        http.with(configurer, Customizer.withDefaults());
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> {
                    if (urls.length > 0) {
                        authorize.requestMatchers(urls).permitAll();
                    }
                    authorize.anyRequest().authenticated();
                })
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .accessDeniedHandler(new AccessDeniedHandlerImpl())
                        .authenticationEntryPoint(new AuthenticationEntryPointImpl()));

        DefaultSecurityFilterChain build = http.build();
        return build;
    }

    @Bean
    @ConditionalOnMissingBean(TokenEncoder.class)
    public TokenEncoder<Authentication, ?> tokeEncoder() {
        PrivateKey privateKey = privateKey();
        return new JwtTokeEncoder(privateKey);
    }

    /**
     * 读取私钥
     *
     * @return
     */

    private PrivateKey privateKey() {
        try (InputStream inputStream = Files.newInputStream(resolveConfFile("rsaPrivateKey.pem"))) {
            return PemUtil.readPemPrivateKey(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Path resolveConfFile(String filename) throws IOException {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("conf").resolve(filename);
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IOException("Cannot find conf/" + filename + " from " + System.getProperty("user.dir"));
    }

    @Bean
    @ConditionalOnMissingBean(IUserService.class)
    public IUserService userService(PasswordEncoder passwordEncoder) {
        return new MemoryUserServiceImpl();
    }


    @Bean
    @ConditionalOnMissingBean(PermissionsService.class)
    public PermissionsService permissionsService() {
        return new MemoryPermissionsServiceImpl();
    }


    @Bean
    @ConditionalOnMissingBean(OauthClientService.class)
    public OauthClientService oauthClientService(PasswordEncoder passwordEncoder) {
        return new OauthClientServiceImpl();
    }

}
