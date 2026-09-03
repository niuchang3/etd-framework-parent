package com.etd.framework.starter.client.autoconfigure;

import com.etd.framework.starter.client.core.permission.PermissionChecker;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/** 使用 Spring Security 方法授权执行权限注解，同时支持原生 @PreAuthorize。 */
@AutoConfiguration(after = SecurityClientAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableMethodSecurity
public class PermissionAutoConfiguration {

    /** 供 @Permission 的原生授权表达式调用。 */
    @Bean
    public PermissionChecker permissionChecker() {
        return new PermissionChecker();
    }
}
