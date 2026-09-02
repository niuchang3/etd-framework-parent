package com.etd.framework.starter.client.core.configurer;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.function.Function;

/**
 * 安全认证端点配置器基类。
 */
public abstract class AbstractSecurityEndpointConfigurer {


    /**
     * 对象后置处理器
     */
    private Function<Object, Object> objectPostProcessor = Function.identity();

    protected void setObjectPostProcessor(Function<Object, Object> objectPostProcessor) {
        this.objectPostProcessor = objectPostProcessor;
    }

    /**
     * 交给 Spring Security 的对象后置处理器处理。
     *
     * @param object 待处理对象
     * @param <T> 对象类型
     * @return 处理后的对象
     */
    /**
     * post 处理执行
     *
     * @param object 参数 object
     * @return 处理结果
     */
    @SuppressWarnings("unchecked")
    protected final <T> T postProcess(T object) {
        return (T) this.objectPostProcessor.apply(object);
    }

    /**
     * 初始化安全配置。
     *
     * @param builder HTTP 安全构建器
     */
    public abstract void init(HttpSecurity builder);

    /**
     * 配置安全端点。
     *
     * @param builder HTTP 安全构建器
     */
    public abstract void configure(HttpSecurity builder);

    /**
     * 获取当前配置器负责的端点匹配器。
     *
     * @return 端点匹配器
     */
    public abstract RequestMatcher getRequestMatcher();

    /**
     * 获取应用上下文。
     *
     * @param httpSecurity HTTP 安全构建器
     * @return Spring 应用上下文
     */
    /**
     * 获取 ApplicationContext 属性值
     *
     * @param httpSecurity 参数 httpSecurity
     * @return 处理结果
     */
    protected ApplicationContext getApplicationContext(HttpSecurity httpSecurity) {
        return httpSecurity.getSharedObject(ApplicationContext.class);
    }

    /**
     * 获取环境变量。
     *
     * @param httpSecurity HTTP 安全构建器
     * @return 环境变量
     */
    /**
     * 获取 Environment 属性值
     *
     * @param httpSecurity 参数 httpSecurity
     * @return 处理结果
     */
    protected Environment getEnvironment(HttpSecurity httpSecurity) {
        return getApplicationContext(httpSecurity).getEnvironment();
    }


    /**
     * 获取认证管理器。
     *
     * @param httpSecurity HTTP 安全构建器
     * @return 认证管理器
     */
    /**
     * 获取 AuthenticationManager 属性值
     *
     * @param httpSecurity 参数 httpSecurity
     * @return 处理结果
     */
    protected AuthenticationManager getAuthenticationManager(HttpSecurity httpSecurity) {
        return httpSecurity.getSharedObject(AuthenticationManager.class);
    }
}
