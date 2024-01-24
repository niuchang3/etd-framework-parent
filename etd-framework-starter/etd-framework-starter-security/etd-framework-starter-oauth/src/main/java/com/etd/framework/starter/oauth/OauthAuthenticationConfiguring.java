package com.etd.framework.starter.oauth;


import com.etd.framework.starter.oauth.authentication.configurer.DefaultOauth2ServerAuthorizationConfigurer;
import com.etd.framework.starter.oauth.authentication.service.Oauth2AuthorizationConsentService;
import com.etd.framework.starter.oauth.authentication.service.Oauth2AuthorizationService;
import com.etd.framework.starter.oauth.authentication.service.Oauth2ClientService;
import com.etd.framework.starter.oauth.authentication.service.UserPasswordService;
import com.etd.framework.starter.oauth.authentication.service.impl.MemoryOauth2AuthorizationConsentServiceImpl;
import com.etd.framework.starter.oauth.authentication.service.impl.MemoryOauth2AuthorizationServiceImpl;
import com.etd.framework.starter.oauth.authentication.service.impl.MemoryOauth2ClientServiceImpl;
import com.etd.framework.starter.oauth.authentication.service.impl.MemoryUserPasswordServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@ComponentScan({"com.etd.framework.starter.oauth.*"})
@EnableConfigurationProperties
public class OauthAuthenticationConfiguring {


    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain defaultAuthenticationServer(HttpSecurity http) throws Exception {
        DefaultOauth2ServerAuthorizationConfigurer.applyDefaultOauth2ServerAuthorization(http);
        DefaultSecurityFilterChain build = http.build();
        return build;
    }

    /**
     * 默认的用户名密码Service
     *
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(UserPasswordService.class)
    public UserPasswordService defaultUserPasswordService() {
        return new MemoryUserPasswordServiceImpl();
    }

    /**
     * 默认的Oauth2客户端信息
     *
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(Oauth2ClientService.class)
    public Oauth2ClientService defaultOauth2Client() {
        return new MemoryOauth2ClientServiceImpl();
    }

    /**
     * 默认的授权信息服务
     *
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(Oauth2AuthorizationService.class)
    public Oauth2AuthorizationService defaultOauth2Authorization() {
        return new MemoryOauth2AuthorizationServiceImpl();
    }

    /**
     * 默认的授权同意接口服务
     *
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(Oauth2AuthorizationConsentService.class)
    public Oauth2AuthorizationConsentService defaultOauth2AuthorizationConsentService() {
        return new MemoryOauth2AuthorizationConsentServiceImpl();
    }
}
