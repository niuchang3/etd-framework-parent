package org.etd.framework.starter.log.dto;

import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.etd.framework.common.core.constants.HeaderConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.etd.framework.starter.log.constant.LogConstant;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志数据模型（兼容 Web HTTP 请求、MQ 消息消费、Job 定时任务及异步线程池等多种环境）
 *
 * @author Young
 * @date 2020/12/14
 */
@Data
public class LogInfo {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
	 * 转换为可直接复制在终端执行的 cURL 命令（仅 HTTP 请求生成）
	 */
	private String curl;

	/**
	 * 用户访问IP
	 */
	private String ip;
	/**
	 * 设备指纹
	 */
	private String deviceFingerprint;
	/**
	 * 设备唯一标识 ID
	 */
	private String deviceId;
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
		fillRequestContext(logInfo);
		fillCurlCommand(logInfo);

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
	 * 填充 MDC / 上下文链路追踪信息
	 */
	private static void fillTraceContext(LogInfo logInfo) {
		logInfo.setTraceId(RequestContext.getTraceId());
	}

	/**
	 * 填充请求上下文（优先全量读取全局 RequestContext，兼容非 HTTP 的 MQ 消费与异步子线程）
	 */
	private static void fillRequestContext(LogInfo logInfo) {
		logInfo.setApplicationName(RequestContext.getApplicationName());
		logInfo.setApplicationVersion(RequestContext.getApplicationVersion());
		logInfo.setDeviceFingerprint(RequestContext.getDeviceFingerprint());
		logInfo.setDeviceId(RequestContext.getDeviceId());
		logInfo.setIp(RequestContext.getRequestIP());

		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
			HttpServletRequest request = servletRequestAttributes.getRequest();
			if (!ObjectUtils.isEmpty(request)) {
				if (request.getRequestURL() != null) {
					logInfo.setUrl(request.getRequestURL().toString());
				}
				logInfo.setUrlMethod(request.getMethod());
			}
		}
		fillUserAgentContext(logInfo);
	}

	/**
	 * 填充 Client User-Agent 客户端软硬件环境信息
	 */
	private static void fillUserAgentContext(LogInfo logInfo) {
		String userAgentStr = RequestContext.getUserAgent();
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
	 * 将请求转换为能在终端直接粘帖运行的 cURL 命令字符串（仅限 Web HTTP 请求场景）
	 */
	private static void fillCurlCommand(LogInfo logInfo) {
		if (!StringUtils.hasText(logInfo.getUrl()) || !StringUtils.hasText(logInfo.getUrlMethod())) {
			return;
		}

		String method = logInfo.getUrlMethod().toUpperCase();
		String url = logInfo.getUrl();

		StringBuilder builder = new StringBuilder("curl -X ").append(method).append(" '").append(url).append("'");

		// 填充 Header
		if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
			builder.append(" -H 'Content-Type: application/json'");
		}
		if (StringUtils.hasText(logInfo.getTraceId())) {
			builder.append(" -H '").append(HeaderConstant.TRACE_ID).append(": ").append(logInfo.getTraceId()).append("'");
		}
		if (RequestContext.getTenantCode() != null) {
			builder.append(" -H '").append(HeaderConstant.TENANT_CODE).append(": ").append(RequestContext.getTenantCode()).append("'");
		}
		if (StringUtils.hasText(RequestContext.getToken())) {
			builder.append(" -H '").append(HeaderConstant.AUTHORIZATION).append(": ").append(RequestContext.getToken()).append("'");
		}
		if (StringUtils.hasText(logInfo.getDeviceFingerprint())) {
			builder.append(" -H '").append(HeaderConstant.DEVICE_FINGERPRINT).append(": ").append(logInfo.getDeviceFingerprint()).append("'");
		}
		if (StringUtils.hasText(logInfo.getDeviceId())) {
			builder.append(" -H '").append(HeaderConstant.DEVICE_ID).append(": ").append(logInfo.getDeviceId()).append("'");
		}
		if (StringUtils.hasText(RequestContext.getLanguage())) {
			builder.append(" -H '").append(HeaderConstant.ACCEPT_LANGUAGE).append(": ").append(RequestContext.getLanguage()).append("'");
		}

		// 填充 Body 参数
		Object params = logInfo.getParameters();
		if (params != null) {
			try {
				String jsonBody = OBJECT_MAPPER.writeValueAsString(params);
				if (StringUtils.hasText(jsonBody) && !"[]".equals(jsonBody) && !"{}".equals(jsonBody)) {
					// 替换单引号防截断
					String safeBody = jsonBody.replace("'", "'\\''");
					builder.append(" --data-raw '").append(safeBody).append("'");
				}
			} catch (JsonProcessingException ignored) {
			}
		}

		logInfo.setCurl(builder.toString());
	}

	/**
	 * 过滤无法直接 JSON 序列化的参数对象（如 Servlet 容器对象、文件流、IO 流、Principal 等），并优化单参数解壳
	 */
	private static Object filterArgs(Object[] args) {
		if (args == null || args.length == 0) {
			return null;
		}
		List<Object> validArgs = new ArrayList<>();
		for (Object arg : args) {
			if (arg == null) {
				continue;
			}
			if (arg instanceof ServletRequest || arg instanceof ServletResponse
					|| arg instanceof MultipartFile || arg instanceof MultipartFile[]
					|| arg instanceof BindingResult
					|| arg instanceof InputStream || arg instanceof OutputStream
					|| arg instanceof Reader || arg instanceof Writer
					|| arg instanceof Principal) {
				continue;
			}
			validArgs.add(arg);
		}
		if (validArgs.isEmpty()) {
			return null;
		}
		// 如果过滤后仅剩下 1 个有效参数（如典型的 Controller 单 DTO 入参），解开外层数组直接返回单对象
		return validArgs.size() == 1 ? validArgs.get(0) : validArgs;
	}
}
