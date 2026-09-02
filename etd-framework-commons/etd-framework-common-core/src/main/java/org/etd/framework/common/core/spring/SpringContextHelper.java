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
@Component
public class SpringContextHelper implements ApplicationContextAware {

    private static ApplicationContext context = null;

    /**
     * 设置 ApplicationContext 属性值
     *
     * @param applicationContext 参数 applicationContext
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }


    /**
     * 获取 Bean 属性值
     *
     * @param name 参数 name
     * @return 处理结果
     */
    public static Object getBean(String name) {
        return context.getBean(name);
    }


    /**
     * 获取 Bean 属性值
     *
     * @param beanClass 参数 beanClass
     * @return 处理结果
     */
    public static Object getBean(Class<?> beanClass) {
        return context.getBean(beanClass);
    }

    /**
     * 获取 ApplicationContext 属性值
     *
     * @return 处理结果
     */
    public static ApplicationContext getApplicationContext() {
        return context;
    }
}
