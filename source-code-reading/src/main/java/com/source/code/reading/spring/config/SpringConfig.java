package com.source.code.reading.spring.config;

import com.source.code.reading.annotation.CustomizeAnnotation;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan("com.source.code.reading.spring.**")
@CustomizeAnnotation(basePackages = "com.source.code.reading.spring.**")
public class SpringConfig {


    @Bean
    public BeanDefinitionRegistryPostProcessor beanFactoryPostProcessor() {
        return new CustomizeBeanFactoryPostProcessor();
    }
}
