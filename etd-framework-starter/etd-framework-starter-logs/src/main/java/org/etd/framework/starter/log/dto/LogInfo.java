package org.etd.framework.starter.log.dto;

import cn.hutool.core.util.URLUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import lombok.Data;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.etd.framework.starter.log.constant.LogConstant;
import org.slf4j.MDC;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author Young
 * @description
 * @date 2020/12/14
 */
@Data
public class LogInfo {
	/**
	 * 日志类型
	 */
	private String logType;
	/**
	 * 操作时间
	 */
	private LocalDateTime timestamp;
	/**
	 * 应用标识
	 */
	private String applicationName;
	/**
	 * 应用版本号
	 */
	private String applicationVersion;
	/**
	 * 类名
	 */
	private String className;
	/**
	 * 方法名称
	 */
	private String classMethodName;


	/**
	 * 用户访问调用链ID
	 */
	private String traceId;

	/**
	 * 操作人
	 */
	private String userId;

	/**
	 * 请求的URL
	 */
	private String url;
	/**
	 * 请求方式
	 */
	private String urlMethod;

	/**
	 * 请求参数
	 */
	private Object parameters;

	/**
	 * 用户访问IP
	 */
	private String ip;
	/**
	 * 是否为移动端访问
	 */
	private Boolean mobile;
	/**
	 * 浏览器
	 */
	private String browser;
	/**
	 * 平台类型
	 */
	private String platform;
	/**
	 * 系统类型
	 */
	private String os;
	/**
	 * 引擎类型
	 */
	private String engine;
	/**
	 * 浏览器版本
	 */
	private String version;
	/**
	 * 引擎版本
	 */
	private String engineVersion;

	/**
	 * 操作内容
	 */
	private String operation;

	/**
	 * 如果发生异常记录异常堆栈信息
	 */
	private String message;


	public static LogInfo getInstance(JoinPoint joinPoint, AutoLog autoLog) {
		return builder(joinPoint, autoLog);
	}

	private static LogInfo builder(JoinPoint joinPoint, AutoLog autoLog) {
		MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

		LogInfo logInfo = new LogInfo();
		logInfo.setLogType(autoLog.logType().name());
		logInfo.setTimestamp(LocalDateTime.now());
		//封装类信息
		logInfo.setClassName(methodSignature.getDeclaringTypeName());
		logInfo.setClassMethodName(methodSignature.getName());

		Map<String, String> copyOfContextMap = MDC.getCopyOfContextMap();
		if(!CollectionUtils.isEmpty(copyOfContextMap)){
			logInfo.setTraceId(copyOfContextMap.containsKey(LogConstant.LOG_TRACE_ID) ? copyOfContextMap.get(LogConstant.LOG_TRACE_ID) : null);
		}
		logInfo.setParameters(joinPoint.getArgs());
		if (!ObjectUtils.isEmpty(requestAttributes)) {
			ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
			if (!ObjectUtils.isEmpty(servletRequestAttributes)) {
				HttpServletRequest request = servletRequestAttributes.getRequest();
				if (!ObjectUtils.isEmpty(request)) {
					logInfo.setApplicationName(request.getHeader(LogConstant.APPLICATION_NAME_HEADER));
					logInfo.setApplicationVersion(request.getHeader(LogConstant.APPLICATION_VERSION_HEADER));
					logInfo.setUrl(URLUtil.getPath(request.getRequestURL().toString()));
					logInfo.setUrlMethod(request.getMethod());
					logInfo.setIp(ServletUtil.getClientIP(request));
					UserAgent parse = UserAgentUtil.parse(request.getHeader(LogConstant.USER_AGENT));
					logInfo.setMobile(parse.isMobile());
					logInfo.setBrowser(parse.getBrowser().toString());
					logInfo.setPlatform(parse.getPlatform().toString());
					logInfo.setOs(parse.getOs().toString());
					logInfo.setEngine(parse.getEngine().toString());
					logInfo.setVersion(parse.getVersion());
					logInfo.setEngineVersion(parse.getEngineVersion());
				}
			}
		}
		logInfo.setOperation(autoLog.value());
		return logInfo;
	}


}
