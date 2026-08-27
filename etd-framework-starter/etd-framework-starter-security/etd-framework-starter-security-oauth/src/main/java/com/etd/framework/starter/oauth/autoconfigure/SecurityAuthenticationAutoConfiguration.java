package com.etd.framework.starter.oauth.autoconfigure;


import cn.hutool.crypto.PemUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.etd.framework.starter.client.core.authentication.bearer.BearerAuthenticationConfigurer;
import com.etd.framework.starter.client.core.configurer.SecurityAuthenticationConfigurer;
import com.etd.framework.starter.client.core.encrypt.TokenEncoder;
import com.etd.framework.starter.client.core.encrypt.impl.JwtTokenEncoder;
import com.etd.framework.starter.client.core.properties.SecurityProperties;
import com.etd.framework.starter.client.core.user.IUserService;
import com.etd.framework.starter.client.core.user.PermissionsService;
import com.etd.framework.starter.client.core.user.memory.MemoryPermissionsServiceImpl;
import com.etd.framework.starter.client.core.user.memory.MemoryUserServiceImpl;
import com.etd.framework.starter.oauth.authentication.AccessDeniedHandlerImpl;
import com.etd.framework.starter.oauth.authentication.AuthenticationEntryPointImpl;
import com.etd.framework.starter.oauth.authentication.internal.configurer.UserPasswordAuthenticationConfigurer;
import com.etd.framework.starter.oauth.authentication.internal.configurer.RefreshTokenAuthenticationConfigurer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.List;

/**
 * 内部登录认证自动配置。
 * <p>
 * 默认注册登录、刷新令牌和 Bearer 访问令牌认证能力，业务系统可以通过自定义 Bean 覆盖默认实现。
 */
@AutoConfiguration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityAuthenticationAutoConfiguration {

    /**
     * 默认安全过滤器链。
     *
     * @param http HTTP 安全构建器
     * @param securityProperties 安全配置
     * @return 安全过滤器链
     */
    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain defaultAuthenticationServer(HttpSecurity http, SecurityProperties securityProperties,
                                                           ObjectMapper objectMapper) throws Exception {
        SecurityAuthenticationConfigurer configurer = new SecurityAuthenticationConfigurer();
        configurer.addConfigurer(new UserPasswordAuthenticationConfigurer().addEndpointMatcher(securityProperties.getAccessToken().getEndpoint()));
        configurer.addConfigurer(new RefreshTokenAuthenticationConfigurer().addEndpointMatcher(securityProperties.getRefreshToken().getEndpoint()));
        configurer.addConfigurer(new BearerAuthenticationConfigurer());

        List<String> ignorePermissions = securityProperties.getPermissions() == null ? null : securityProperties.getPermissions().getIgnore();
        String[] urls = CollectionUtils.isEmpty(ignorePermissions) ? new String[0] : ignorePermissions.toArray(String[]::new);
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
                        .accessDeniedHandler(new AccessDeniedHandlerImpl(objectMapper))
                        .authenticationEntryPoint(new AuthenticationEntryPointImpl(objectMapper)));

        return http.build();
    }

    /**
     * 默认 JWT 令牌签发器。
     *
     * @param securityProperties 安全配置
     * @return 令牌签发器
     */
    @Bean
    @ConditionalOnMissingBean(TokenEncoder.class)
    public TokenEncoder<Authentication, ?> tokenEncoder(SecurityProperties securityProperties) {
        PrivateKey privateKey = privateKey(securityProperties);
        return new JwtTokenEncoder(privateKey, securityProperties);
    }

    /**
     * 从配置路径读取 RSA 私钥。
     *
     * @param securityProperties 安全配置
     * @return RSA 私钥
     */
    private PrivateKey privateKey(SecurityProperties securityProperties) {
        String location = getPrivateKeyLocation(securityProperties);
        try (InputStream inputStream = Files.newInputStream(resolveConfFile(location))) {
            return PemUtil.readPemPrivateKey(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("读取私钥配置失败。", e);
        }
    }

    /**
     * 从配置中获取私钥路径。
     *
     * @param securityProperties 安全配置
     * @return 私钥文件路径
     */
    private String getPrivateKeyLocation(SecurityProperties securityProperties) {
        if (securityProperties.getKey() == null || !StringUtils.hasText(securityProperties.getKey().getPrivateKeyPath())) {
            return "conf/rsaPrivateKey.pem";
        }
        return securityProperties.getKey().getPrivateKeyPath();
    }

    /**
     * 解析密钥文件路径。
     * <p>
     * 绝对路径直接使用；相对路径会优先按当前运行目录解析，找不到时再从当前目录向上查找。
     *
     * @param location 配置文件路径
     * @return 配置文件路径
     */
    private Path resolveConfFile(String location) throws IOException {
        Path configured = Paths.get(location);
        if (configured.isAbsolute()) {
            if (Files.exists(configured)) {
                return configured;
            }
            throw new IOException("未找到配置的私钥文件：" + configured);
        }

        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(configured);
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IOException("从当前目录向上查找，未找到配置文件 " + location + "，当前目录：" + System.getProperty("user.dir"));
    }

    /**
     * 默认内存用户服务。
     *
     * @return 用户服务
     */
    @Bean
    @ConditionalOnMissingBean(IUserService.class)
    public IUserService userService() {
        return new MemoryUserServiceImpl();
    }


    /**
     * 默认内存权限服务。
     *
     * @return 权限服务
     */
    @Bean
    @ConditionalOnMissingBean(PermissionsService.class)
    public PermissionsService permissionsService() {
        return new MemoryPermissionsServiceImpl();
    }

}
