package com.etd.framework.starter.oauth.authentication.configurer.extend;

import com.etd.framework.starter.oauth.authentication.configurer.AbstractAuthorizationConfigurer;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Oauth2 token 配置器
 * <p>
 * 负责处理请求 /oauth2/token
 */
public class Oauth2TokenEndpointConfigurer extends AbstractAuthorizationConfigurer {


    public Oauth2TokenEndpointConfigurer(ObjectPostProcessor<Object> objectPostProcessor) {
        super(objectPostProcessor);
    }

    @Override
    public void init(HttpSecurity builder) {

    }

    @Override
    public void configure(HttpSecurity builder) {

    }

    @Override
    public RequestMatcher getRequestMatcher() {
        return null;
    }
}
