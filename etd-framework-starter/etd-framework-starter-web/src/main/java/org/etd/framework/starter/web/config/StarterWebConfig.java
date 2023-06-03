package org.etd.framework.starter.web.config;

import org.etd.framework.starter.web.exception.GlobalBasicErrorController;
import org.etd.framework.starter.web.interceptor.CustomInterceptor;
import org.etd.framework.starter.web.interceptor.extend.CustomDefaultInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class StarterWebConfig {

    @Bean
    @ConditionalOnMissingBean(CustomInterceptor.class)
    public CustomInterceptor initCustomInterceptor() {
        return new CustomDefaultInterceptor();
    }


    @Bean()
    @Order(1)
    public ErrorController globalBasicErrorController() {
        return new GlobalBasicErrorController();
    }

}
