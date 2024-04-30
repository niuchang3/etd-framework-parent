package org.etd.framework.starter.web.config;

import org.etd.framework.starter.web.interceptor.EtdFrameworkHttpRequestInterceptor;
import org.etd.framework.starter.web.interceptor.extend.EtdFrameworkHttpRequestInterceptorImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class StarterWebConfig {

    @Bean
    @ConditionalOnMissingBean(EtdFrameworkHttpRequestInterceptor.class)
    public EtdFrameworkHttpRequestInterceptor initCustomInterceptor() {
        EtdFrameworkHttpRequestInterceptorImpl interceptor = new EtdFrameworkHttpRequestInterceptorImpl();
        interceptor.addWhiteList(new AntPathRequestMatcher("/*/user/tenant"));
        return interceptor;
    }


}
