package org.etd.framework.common.core.context;

import jakarta.servlet.http.HttpServletRequest;
import org.etd.framework.common.core.constants.HeaderConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.context.model.RequestHeaderContext;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.common.utils.ip.IpUtil;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.Enumeration;
import java.util.Map;
import java.util.UUID;

/**
 * 统一请求上下文适配器与初始化器
 * 支持 Web HTTP 适配、MQ/RPC Header 组装以及全量无损还原
 *
 * @author Young
 */
public class RequestContextInitializer {

    /**
     * Web HTTP 请求场景全量与动态上下文初始化
     *
     * @param request HttpServletRequest 请求对象
     */
    public static void init(HttpServletRequest request) {
        if (request == null) {
            return;
        }

        // 1. 进场清理：确保写入新数据前，当前线程无任何历史遗留数据
        RequestContext.clean();

        // 2. 解析并设置 TraceId（为空时自动生成 UUID 补全）
        String traceId = request.getHeader(HeaderConstant.TRACE_ID);
        RequestContext.setTraceId(StringUtils.hasText(traceId) ? traceId.trim() : UUID.randomUUID().toString());

        // 3. 解析并设置租户编码
        String tenantCodeStr = request.getHeader(HeaderConstant.TENANT_CODE);
        if (StringUtils.hasText(tenantCodeStr)) {
            try {
                RequestContext.setTenantCode(Long.valueOf(tenantCodeStr.trim()));
            } catch (NumberFormatException ignored) {
            }
        }

        // 4. 解析并设置核心标准标头与请求元数据
        RequestContext.setToken(request.getHeader(HeaderConstant.AUTHORIZATION));
        RequestContext.setLanguage(request.getHeader(HeaderConstant.ACCEPT_LANGUAGE));
        RequestContext.setApplicationName(request.getHeader(HeaderConstant.APPLICATION_NAME));
        RequestContext.setApplicationVersion(request.getHeader(HeaderConstant.APPLICATION_VERSION));
        RequestContext.setDeviceFingerprint(request.getHeader(HeaderConstant.DEVICE_FINGERPRINT));
        RequestContext.setDeviceId(request.getHeader(HeaderConstant.DEVICE_ID));
        RequestContext.setUserAgent(request.getHeader(HeaderConstant.USER_AGENT));

        // 5. 解析并设置客户端真实 IP
        RequestContext.setRequestIP(IpUtil.getRemoteIp(request));

        // 6. 从 Spring Security 安全上下文中提取当前已认证的 UserDetails 信息
        SecurityContext context = SecurityContextHolder.getContext();
        if (context != null && context.getAuthentication() != null) {
            Object details = context.getAuthentication().getDetails();
            if (details instanceof UserDetails userDetails) {
                RequestContext.setUser(userDetails);
            }
        }

        // 7. 遍历所有 HTTP 请求头，将自定义或扩展 Header 自动动态装载进 attributes Map 中
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                if (RequestHeaderContext.isExtensionHeader(name) && !RequestHeaderContext.isKnownHeader(name)) {
                    String val = request.getHeader(name);
                    if (StringUtils.hasText(val)) {
                        RequestContext.setAttribute(name, val);
                    }
                }
            }
        }
    }

    /**
     * Map / MQ / RPC / 异构请求全量与动态上下文初始化
     * 一键无损还原所有传输消息头与扩展属性
     *
     * @param headers 标头映射 Map
     */
    public static void init(Map<String, ?> headers) {
        RequestContext.clean();
        if (ObjectUtils.isEmpty(headers)) {
            RequestContext.setTraceId(UUID.randomUUID().toString());
            return;
        }

        // 直接调用 RequestHeaderContext 自身的无损还原与大小写兼容能力
        RequestHeaderContext headerContext = RequestContext.getHeaderContext();
        headerContext.fromMap(headers);

        // 如果传入标头中缺乏 traceId，进行 UUID 兜底补全
        if (!StringUtils.hasText(headerContext.getTraceId())) {
            headerContext.setTraceId(UUID.randomUUID().toString());
        }
    }

    /**
     * 导出当前 RequestContext 中的全量及动态扩展 Header 标头，用于 MQ 发送、RPC 远程调用、线程池透传
     * 干净、全量导出网络传输标头，绝对不混入复杂的 UserDetails 对象
     *
     * @return 包含全量核心 Header 及动态扩展 Header 的 Map
     */
    public static Map<String, Object> exportHeaders() {
        return RequestContext.getHeaderContext().toMap();
    }

    /**
     * 导出消息队列安全上下文。消息中不传递用户认证 Token，但保留标准化请求 IP。
     */
    public static Map<String, Object> exportMessageHeaders() {
        Map<String, Object> headers = RequestContext.getHeaderContext().toMap();
        headers.keySet().removeIf(key -> HeaderConstant.AUTHORIZATION.equalsIgnoreCase(key));
        return headers;
    }
}
