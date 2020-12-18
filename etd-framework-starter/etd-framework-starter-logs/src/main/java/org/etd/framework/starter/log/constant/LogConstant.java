package org.etd.framework.starter.log.constant;

import java.io.Serializable;

/**
 * @author Young
 * @description
 * @date 2020/9/15
 */
public class LogConstant implements Serializable {

	/**
	 * 接受上游
	 */
	public static final String TRACE_ID_HEADER = "x-traceId-header";
	/**
	 * 服务信息
	 */
	public static final String APPLICATION_NAME_HEADER = "x-application-header";
	/**
	 * 服务信息版本信息
	 */
	public static final String APPLICATION_VERSION_HEADER = "x-application-version-header";
	/**
	 * 代理信息
	 */
	public static final String USER_AGENT = "User-Agent";
	/**
	 * 日志链路追踪id日志标志
	 */
	public static final  String LOG_TRACE_ID = "traceId";
	/**
	 * 日志类型
	 */
	public enum LOG_TYPE {
		/**
		 * 系统异常日志
		 */
		error,
		/**
		 * 访问日志
		 */
		access;

	}
}
