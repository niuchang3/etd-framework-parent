package org.etd.framework.starter.log.dto;

import cn.hutool.core.util.URLUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import lombok.Data;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.etd.framework.common.core.constants.HeaderConstant;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.etd.framework.starter.log.constant.LogConstant;
import org.slf4j.MDC;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 操作日志数据模型
 *
 * @author Young
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
	 * 接口耗时（毫秒）
	 */
	private Long costTime;
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
	 * 操作人ID
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
	 * 异常信息
	 */
	private String message;


	public static LogInfo getInstance(JoinPoint joinPoint, AutoLog autoLog) {
		return builder(joinPoint, autoLog);
	}

	private static LogInfo builder(JoinPoint joinPoint, AutoLog autoLog) {
		LogInfo logInfo = new LogInfo();
		logInfo.setTimestamp(LocalDateTime.now());

		fillAutoLogMetadata(logInfo, autoLog);
		fillMethodMetadata(logInfo, joinPoint);
		fillTraceContext(logInfo);
		fillHttpRequestContext(logInfo);

		return logInfo;
	}

	/**
	 * 填充 AutoLog 注解元数据
	 */
	private static void fillAutoLogMetadata(LogInfo logInfo, AutoLog autoLog) {
		if (autoLog != null && autoLog.logType() != null) {
			logInfo.setLogType(autoLog.logType().getCode());
			logInfo.setOperation(autoLog.value());
		} else {
			logInfo.setLogType(LogConstant.LOG_TYPE.ACCESS.getCode());
		}
	}

	/**
	 * 填充切点类方法与参数信息
	 */
	private static void fillMethodMetadata(LogInfo logInfo, JoinPoint joinPoint) {
		if (joinPoint.getSignature() instanceof MethodSignature methodSignature) {
			logInfo.setClassName(methodSignature.getDeclaringTypeName());
			logInfo.setClassMethodName(methodSignature.getName());
		}
		logInfo.setParameters(filterArgs(joinPoint.getArgs()));
	}

	/**
	 * 填充 MDC 链路追踪信息
	 */
	private static void fillTraceContext(LogInfo logInfo) {
		Map<String, String> copyOfContextMap = MDC.getCopyOfContextMap();
		if (!CollectionUtils.isEmpty(copyOfContextMap)) {
			logInfo.setTraceId(copyOfContextMap.get(LogConstant.LOG_TRACE_ID));
		}
	}

	/**
	 * 填充 HTTP 请求上下文（URL、Method、IP、Header 等）
	 */
	private static void fillHttpRequestContext(LogInfo logInfo) {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
			HttpServletRequest request = servletRequestAttributes.getRequest();
			if (!ObjectUtils.isEmpty(request)) {
				logInfo.setApplicationName(request.getHeader(HeaderConstant.APPLICATION_NAME));
				logInfo.setApplicationVersion(request.getHeader(HeaderConstant.APPLICATION_VERSION));
				if (request.getRequestURL() != null) {
					logInfo.setUrl(URLUtil.getPath(request.getRequestURL().toString()));
				}
				logInfo.setUrlMethod(request.getMethod());
				logInfo.setIp(getClientIp(request));

				fillUserAgentContext(logInfo, request);
			}
		}
	}

	/**
	 * 填充 Client User-Agent 客户端软硬件环境信息
	 */
	private static void fillUserAgentContext(LogInfo logInfo, HttpServletRequest request) {
		String userAgentStr = request.getHeader(HeaderConstant.USER_AGENT);
		if (StringUtils.hasText(userAgentStr)) {
			UserAgent parse = UserAgentUtil.parse(userAgentStr);
			if (parse != null) {
				logInfo.setMobile(parse.isMobile());
				logInfo.setBrowser(parse.getBrowser() != null ? parse.getBrowser().toString() : "Unknown");
				logInfo.setPlatform(parse.getPlatform() != null ? parse.getPlatform().toString() : "Unknown");
				logInfo.setOs(parse.getOs() != null ? parse.getOs().toString() : "Unknown");
				logInfo.setEngine(parse.getEngine() != null ? parse.getEngine().toString() : "Unknown");
				logInfo.setVersion(parse.getVersion());
				logInfo.setEngineVersion(parse.getEngineVersion());
			}
		}
	}

	/**
	 * 过滤无法直接 JSON 序列化的参数对象（如 HttpServletRequest, MultipartFile 等）
	 */
	private static List<Object> filterArgs(Object[] args) {
		List<Object> validArgs = new ArrayList<>();
		if (args == null || args.length == 0) {
			return validArgs;
		}
		for (Object arg : args) {
			if (arg == null) {
				continue;
			}
			if (arg instanceof ServletRequest || arg instanceof ServletResponse
					|| arg instanceof MultipartFile || arg instanceof MultipartFile[]
					|| arg instanceof BindingResult) {
				continue;
			}
			validArgs.add(arg);
		}
		return validArgs;
	}

	private static String getClientIp(HttpServletRequest request) {
		for (String header : HeaderConstant.IP_HEADERS) {
			String value = request.getHeader(header);
			if (!ObjectUtils.isEmpty(value) && !"unknown".equalsIgnoreCase(value)) {
				int commaIndex = value.indexOf(',');
				return commaIndex > -1 ? value.substring(0, commaIndex).trim() : value.trim();
			}
		}
		return request.getRemoteAddr();
	}
}
