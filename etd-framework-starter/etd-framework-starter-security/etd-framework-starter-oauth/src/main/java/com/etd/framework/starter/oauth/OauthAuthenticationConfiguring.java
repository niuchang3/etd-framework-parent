package com.etd.framework.starter.oauth;


import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@ComponentScan({"com.etd.framework.starter.oauth.authentication.*"})
@EnableConfigurationProperties
public class OauthAuthenticationConfiguring {




    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain defaultAuthenticationServer(HttpSecurity http) throws Exception {

//        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
//        BasicAuthorizationServerConfiguration.applyDefaultBasicSecurity(http);
        return http.build();
    }
}
