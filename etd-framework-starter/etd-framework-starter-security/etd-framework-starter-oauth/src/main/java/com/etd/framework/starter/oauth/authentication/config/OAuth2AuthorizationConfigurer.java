package com.etd.framework.starter.oauth.authentication.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

public class OAuth2AuthorizationConfigurer extends AbstractHttpConfigurer<OAuth2AuthorizationConfigurer, HttpSecurity> {


    @Override
    public void init(HttpSecurity builder) throws Exception {
        super.init(builder);
    }

    @Override
    public void configure(HttpSecurity builder) throws Exception {
        super.configure(builder);
    }
}
