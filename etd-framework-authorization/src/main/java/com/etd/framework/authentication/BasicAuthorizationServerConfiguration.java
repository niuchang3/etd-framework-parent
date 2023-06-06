package com.etd.framework.authentication;


import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.DefaultLoginPageConfigurer;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;


public class BasicAuthorizationServerConfiguration {


    public static void applyDefaultBasicSecurity(HttpSecurity http) throws Exception {

        // 从HttpSecurity中移除 默认的登录页配置
        http.removeConfigurer(DefaultLoginPageConfigurer.class);
        // 移除默认的退出登录
        http.removeConfigurer(LogoutConfigurer.class);
        //移除异常处理配置
        http.removeConfigurer(ExceptionHandlingConfigurer.class);
        // 添加基础认证Configuration
        BasicAuthenticationConfiguration configuration = new BasicAuthenticationConfiguration();
        http.apply(configuration)
                .and()
                // 关闭csrf校验
                .csrf().ignoringRequestMatchers(configuration.getEndpointsMatcher())
                .and()
                // 匹配端点
                .requestMatcher(configuration.getEndpointsMatcher());
    }
}
