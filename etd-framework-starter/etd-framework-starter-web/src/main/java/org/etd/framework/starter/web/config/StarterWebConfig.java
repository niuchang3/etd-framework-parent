package org.etd.framework.starter.web.config;

import org.etd.framework.starter.web.interceptor.EtdFrameworkHttpRequestInterceptor;
import org.etd.framework.starter.web.interceptor.extend.EtdFrameworkHttpRequestInterceptorImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Web Starter 默认拦截器自动配置
 *
 * @author Young
 */
@AutoConfiguration
public class StarterWebConfig {

    @Bean
    @ConditionalOnMissingBean(EtdFrameworkHttpRequestInterceptor.class)
    public EtdFrameworkHttpRequestInterceptor initCustomInterceptor() {
        EtdFrameworkHttpRequestInterceptorImpl interceptor = new EtdFrameworkHttpRequestInterceptorImpl();
        // 租户列表接口不参与租户权限校验
        interceptor.addWhiteList("/*/user/tenant");
        return interceptor;
    }
}
