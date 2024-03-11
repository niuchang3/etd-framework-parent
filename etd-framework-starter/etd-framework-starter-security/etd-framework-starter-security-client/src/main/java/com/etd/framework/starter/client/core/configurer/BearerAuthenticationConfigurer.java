package com.etd.framework.starter.client.core.configurer;

import com.etd.framework.starter.client.core.AbstractHttpSecurityConfigurer;
import com.etd.framework.starter.client.core.converter.BearerAuthenticationConverter;
import com.etd.framework.starter.client.core.encrypt.impl.JwtTokenDecode;
import com.etd.framework.starter.client.core.filter.BearerTokenAuthenticationFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.util.matcher.RequestMatcher;


public class BearerAuthenticationConfigurer extends AbstractHttpSecurityConfigurer {


    public BearerAuthenticationConfigurer() {
        this(null);
    }

    public BearerAuthenticationConfigurer(ObjectPostProcessor<Object> objectPostProcessor) {
        super(objectPostProcessor);
    }


    @Override
    public void init(HttpSecurity builder) {

    }

    @Override
    public void configure(HttpSecurity builder) {
        ApplicationContext applicationContext = getApplicationContext(builder);
        JwtTokenDecode JwtTokenDecode = applicationContext.getBean(JwtTokenDecode.class);


        AuthenticationConverter converter = new BearerAuthenticationConverter(JwtTokenDecode);
        BearerTokenAuthenticationFilter filter = new BearerTokenAuthenticationFilter(converter);
        builder.addFilterBefore(filter, FilterSecurityInterceptor.class);
    }

    @Override
    public RequestMatcher getRequestMatcher() {
        return null;
    }
}
