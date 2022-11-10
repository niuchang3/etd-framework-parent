package com.source.code.reading.annotation;

import com.source.code.reading.spring.config.CustomizeImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(CustomizeImportBeanDefinitionRegistrar.class)
public @interface CustomizeAnnotation {

    /**
     * 扫描路径
     *
     * @return
     */
    String[] basePackages() default {};
}
