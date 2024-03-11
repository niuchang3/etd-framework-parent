package com.etd.framework.starter.client.core;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * 抽象的HTTP 安全配置器
 */
public abstract class AbstractHttpSecurityConfigurer {


    /**
     * 对象后置处理器
     */
    private ObjectPostProcessor<Object> objectPostProcessor;


    public AbstractHttpSecurityConfigurer(ObjectPostProcessor<Object> objectPostProcessor) {
        this.objectPostProcessor = objectPostProcessor;
    }

    protected void setObjectPostProcessor(ObjectPostProcessor<Object> objectPostProcessor){
        this.objectPostProcessor = objectPostProcessor;
    }

    protected final <T> T postProcess(T object) {
        return (T) this.objectPostProcessor.postProcess(object);
    }

    /**
     * 初始化调用
     *
     * @param builder
     * @throws Exception
     */
    public abstract void init(HttpSecurity builder);

    /**
     * 配置Config
     *
     * @param builder
     * @throws Exception
     */
    public abstract void configure(HttpSecurity builder);

    /**
     * 获取拦截端点
     *
     * @return
     */
    public abstract RequestMatcher getRequestMatcher();

    /**
     * 获取应用上下文
     *
     * @param httpSecurity
     * @return
     */
    protected ApplicationContext getApplicationContext(HttpSecurity httpSecurity) {
        return httpSecurity.getSharedObject(ApplicationContext.class);
    }

    /**
     * 获取环境变量
     *
     * @param httpSecurity
     * @return
     */
    protected Environment getEnvironment(HttpSecurity httpSecurity) {
        return getApplicationContext(httpSecurity).getEnvironment();
    }


    /**
     * 获取认证管理器
     *
     * @param httpSecurity
     * @return
     */
    protected AuthenticationManager getAuthenticationManager(HttpSecurity httpSecurity) {
        return httpSecurity.getSharedObject(AuthenticationManager.class);
    }
}
