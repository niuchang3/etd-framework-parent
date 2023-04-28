package org.etd.framework.starter.mybaits.fill;


import org.etd.framework.starter.mybaits.fill.handler.DataFillHandler;
import org.etd.framework.starter.mybaits.fill.handler.extend.DefaultDataFillHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class DataFillAutoConfiguration {


    /**
     * 当开启默认填充功能，并且默认填充实现类为空的时候进行自动配置
     *
     * @return
     */
    @ConditionalOnProperty(prefix = "etd.mybatis.fill", value = "enabled",matchIfMissing = true)
    @ConditionalOnMissingBean(DataFillHandler.class)
    @Bean
    public DataFillHandler defaultDataFillHandler() {
        return new DefaultDataFillHandler();
    }
}
