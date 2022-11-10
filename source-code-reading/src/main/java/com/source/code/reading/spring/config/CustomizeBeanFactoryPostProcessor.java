package com.source.code.reading.spring.config;

import com.source.code.reading.spring.bean.TestBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;


public class CustomizeBeanFactoryPostProcessor implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        System.out.println("1");
    }


    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(TestBean.class).getBeanDefinition();

        BeanDefinitionHolder beanDefinitionHolder = new BeanDefinitionHolder(beanDefinition, "testBean");

        BeanDefinitionReaderUtils.registerBeanDefinition(beanDefinitionHolder, registry);
    }
}
