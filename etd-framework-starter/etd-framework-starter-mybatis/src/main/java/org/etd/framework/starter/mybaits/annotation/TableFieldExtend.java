package org.etd.framework.starter.mybaits.annotation;

import java.lang.annotation.*;

/**
 * 扩展MyBatisTableField，用于作为默认值填充使用。
 * 使用方式：
 * 1、从SpringBeanFactory中获取bean并调用方法，语法糖：@demo.xxx()，需要注意不支持从Xml中配置的Bean
 * 2、从静态方法中获取数据，T(xxx.xxx.xxx.Demo).xxx()
 * 3、直接创建某个对象，new xxx.xxx.xxx.Demo()
 * 5、详情参照 SpEL 表达式
 *
 *
 * 全局导出Bean
 * @author Young
 * @description，
 * @date 2020/6/16
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TableFieldExtend {

	/**
	 * @return
	 */
	String value() default "";

	/**
	 *  表达式，用于填充val信息
	 */
	String expression() default "";

}
