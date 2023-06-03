package com.etd.framework.config;

import com.etd.framework.authentication.BasicAuthenticationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.DefaultLoginPageConfigurer;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {


    @Bean
    public SecurityFilterChain defaultAuthenticationServer(HttpSecurity http) throws Exception {
        http.removeConfigurer(DefaultLoginPageConfigurer.class);
        http.removeConfigurer(LogoutConfigurer.class);
        http.removeConfigurer(ExceptionHandlingConfigurer.class);


//        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        BasicAuthenticationConfiguration configuration = new BasicAuthenticationConfiguration();
        http.apply(configuration);

//        http.exceptionHandling()
        return http.build();
    }
}
