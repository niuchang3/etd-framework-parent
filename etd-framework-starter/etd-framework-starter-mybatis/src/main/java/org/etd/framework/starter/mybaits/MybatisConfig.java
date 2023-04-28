package org.etd.framework.starter.mybaits;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
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
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }

}
