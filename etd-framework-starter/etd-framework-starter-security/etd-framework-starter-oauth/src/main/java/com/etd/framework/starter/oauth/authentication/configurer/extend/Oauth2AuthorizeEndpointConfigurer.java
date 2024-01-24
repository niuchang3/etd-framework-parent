package com.etd.framework.starter.oauth.authentication.configurer.extend;

import com.etd.framework.starter.oauth.authentication.configurer.AbstractAuthorizationConfigurer;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Oauth2 授权端点配置器
 * <p>
 * /oauth2/authorize
 */
public class Oauth2AuthorizeEndpointConfigurer extends AbstractAuthorizationConfigurer {


    public Oauth2AuthorizeEndpointConfigurer(ObjectPostProcessor<Object> objectPostProcessor) {
        super(objectPostProcessor);
    }

    @Override
    public void init(HttpSecurity builder){

    }

    @Override
    public void configure(HttpSecurity builder){

    }

    @Override
    public RequestMatcher getRequestMatcher() {
        return null;
    }
}
