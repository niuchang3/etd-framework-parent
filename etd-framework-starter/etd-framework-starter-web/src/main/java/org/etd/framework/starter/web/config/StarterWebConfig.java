package org.etd.framework.starter.web.config;

import org.etd.framework.starter.web.interceptor.EtdFrameworkHttpRequestInterceptor;
import org.etd.framework.starter.web.interceptor.extend.EtdFrameworkHttpRequestInterceptorImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
public class StarterWebConfig {

    private static final PathPatternRequestMatcher.Builder PATH_MATCHER = PathPatternRequestMatcher.withDefaults();

    @Bean
    @ConditionalOnMissingBean(EtdFrameworkHttpRequestInterceptor.class)
    public EtdFrameworkHttpRequestInterceptor initCustomInterceptor() {
        EtdFrameworkHttpRequestInterceptorImpl interceptor = new EtdFrameworkHttpRequestInterceptorImpl();
        // 租户列表接口不参与租户权限校验。
        interceptor.addWhiteList(PATH_MATCHER.matcher("/*/user/tenant"));
        return interceptor;
    }
}
