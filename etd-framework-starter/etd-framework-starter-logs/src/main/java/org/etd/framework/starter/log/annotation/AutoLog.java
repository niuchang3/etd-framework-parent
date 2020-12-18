package org.etd.framework.starter.log.annotation;

import org.etd.framework.starter.log.constant.LogConstant;

import java.lang.annotation.*;

/**
 * @author Young
 * @description
 * @date 2020/9/15
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoLog {

	/**
	 * 日志类型默认为访问日志
	 *
	 * @return
	 */
	LogConstant.LOG_TYPE logType() default LogConstant.LOG_TYPE.access;

	/**
	 * 日志内容
	 *
	 * @return
	 */
	String value() default "";
}
