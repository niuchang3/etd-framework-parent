package org.etd.framework.starter.mybaits;

import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.etd.framework.starter.mybaits.tenant.EtdTenantLineHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author Young  经验
 * @description
 * @date 2020/6/23
 */

@Configuration
@ComponentScan({"org.etd.framework.starter.mybaits.*", "com.baomidou.mybatisplus.core.mapper"})
@EnableConfigurationProperties(value = EtdMyBatisPlusProperties.class)
public class MybatisConfig {


    /**
     * MybatisPlus 拦截器配置
     *
     * @return
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        TenantLineInnerInterceptor tenantLineInnerInterceptor = new TenantLineInnerInterceptor();
        tenantLineInnerInterceptor.setTenantLineHandler(new EtdTenantLineHandler(""));
//        tenantLineInnerInterceptor.setProperties();


        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

//        tenantLineInnerInterceptor.setTenantLineHandler();
        interceptor.addInnerInterceptor(tenantLineInnerInterceptor);
        return interceptor;
    }
//
//    @Bean
//    public ConfigurationCustomizer configurationCustomizer() {
//        return configuration -> configuration.setUsr
//    }

}
