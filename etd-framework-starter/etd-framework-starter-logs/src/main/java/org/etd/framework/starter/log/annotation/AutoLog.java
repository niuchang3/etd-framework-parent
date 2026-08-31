package org.etd.framework.starter.log.annotation;

import org.etd.framework.starter.log.constant.LogConstant;

import java.lang.annotation.*;

/**
 * 自动日志注解
 *
 * @author Young
 * @date 2020/9/15
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoLog {

	/**
	 * 日志类型默认为访问日志
	 *
	 * @return 日志类型
	 */
	LogConstant.LOG_TYPE logType() default LogConstant.LOG_TYPE.ACCESS;

	/**
	 * 日志描述内容
	 *
	 * @return 描述
	 */
	String value() default "";
}
