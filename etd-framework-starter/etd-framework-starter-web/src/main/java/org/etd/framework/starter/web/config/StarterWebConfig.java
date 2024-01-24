package org.etd.framework.starter.web.config;

import org.etd.framework.starter.web.interceptor.CustomInterceptor;
import org.etd.framework.starter.web.interceptor.extend.CustomDefaultInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StarterWebConfig {

    @Bean
    @ConditionalOnMissingBean(CustomInterceptor.class)
    public CustomInterceptor initCustomInterceptor() {
        return new CustomDefaultInterceptor();
    }


}
