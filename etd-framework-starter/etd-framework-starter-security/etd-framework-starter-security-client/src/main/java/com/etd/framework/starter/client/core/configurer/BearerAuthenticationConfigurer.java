package com.etd.framework.starter.client.core.configurer;

import com.etd.framework.starter.client.core.AbstractHttpSecurityConfigurer;
import com.etd.framework.starter.client.core.converter.BearerAuthenticationConverter;
import com.etd.framework.starter.client.core.encrypt.TokenDecode;
import com.etd.framework.starter.client.core.encrypt.impl.JwtTokenDecode;
import com.etd.framework.starter.client.core.filter.BearerTokenAuthenticationFilter;
import com.etd.framework.starter.client.core.provider.BearerAuthenticationProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
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
        TokenDecode tokenDecode = getApplicationContext(builder).getBean(TokenDecode.class);
        BearerAuthenticationProvider provider = new BearerAuthenticationProvider(tokenDecode);
        builder.authenticationProvider(provider);
    }

    @Override
    public void configure(HttpSecurity builder) {
        AuthenticationConverter converter = new BearerAuthenticationConverter();
        BearerTokenAuthenticationFilter filter = new BearerTokenAuthenticationFilter(converter,getAuthenticationManager(builder));
        builder.addFilterAfter(filter, AnonymousAuthenticationFilter.class);
    }

    @Override
    public RequestMatcher getRequestMatcher() {
        return null;
    }
}
