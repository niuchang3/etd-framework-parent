package org.etd.framework.common.core.spring;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;


/**
 * 获取SpringContext上下文
 *
 * @author Administrator
 */
public class SpringContextHelper implements ApplicationContextAware {

    private static ApplicationContext context = null;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }


    public static Object getBean(String name) {
        return context.getBean(name);
    }


    public static Object getBean(Class<?> beanClass) {
        return context.getBean(beanClass);
    }

    public static ApplicationContext getApplicationContext() {
        return context;
    }
}