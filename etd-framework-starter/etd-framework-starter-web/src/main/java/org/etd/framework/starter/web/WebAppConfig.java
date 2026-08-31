package org.etd.framework.starter.web;

import org.etd.framework.starter.web.interceptor.EtdFrameworkHttpRequestInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 自动配置类
 *
 * @author Young
 */
@Order(0)
@AutoConfiguration
public class WebAppConfig implements WebMvcConfigurer {

    @Autowired
    private EtdFrameworkHttpRequestInterceptor etdFrameworkHttpRequestInterceptor;

    /**
     * 注册 HTTP 请求处理拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (etdFrameworkHttpRequestInterceptor != null) {
            registry.addInterceptor(etdFrameworkHttpRequestInterceptor)
                    .addPathPatterns(etdFrameworkHttpRequestInterceptor.getInterceptorsPath());
        }
    }

    /**
     * 静态资源映射
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");

        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}
