package org.etd.framework.starter.mybaits;

import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.etd.framework.starter.mybaits.tenant.EtdTenantLineHandler;
import org.etd.framework.starter.mybaits.tenant.aspect.IgnoreTenantAspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

    private final EtdMyBatisPlusProperties properties;

    public MybatisConfig(EtdMyBatisPlusProperties properties) {
        this.properties = properties;
    }

    /**
     * MybatisPlus 拦截器配置
     *
     * @return
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        if(properties.getTenant().getEnabled()){
            TenantLineInnerInterceptor tenantLineInnerInterceptor = new TenantLineInnerInterceptor();
            EtdTenantLineHandler etdTenantLineHandler = new EtdTenantLineHandler(properties.getTenant().getColumnName());
            etdTenantLineHandler.addIgnoreTables(properties.getTenant().getIgnoreTables());
            tenantLineInnerInterceptor.setTenantLineHandler(etdTenantLineHandler);
            interceptor.addInnerInterceptor(tenantLineInnerInterceptor);
        }

        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        return interceptor;
    }


    @ConditionalOnProperty(prefix = "etd.mybatis.tenant", value = "enabled",matchIfMissing = true)
    @Bean
    public IgnoreTenantAspect ignoreTenantAspect(){
        return new IgnoreTenantAspect();
    }

}
