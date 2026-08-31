package org.etd.framework.common.core.context;

import jakarta.servlet.http.HttpServletRequest;
import org.etd.framework.common.core.constants.HeaderConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.common.utils.request.RequestUtil;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 统一请求上下文适配器与初始化器
 * 支持强类型核心标头与开箱即用的动态扩展标头 (Attribute Map)
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
        RequestContext.setRequestIP(RequestUtil.getRemoteIp(request));

        // 6. 从 Spring Security 安全上下文中提取当前已认证的 UserDetails 信息
        SecurityContext context = SecurityContextHolder.getContext();
        if (context != null && context.getAuthentication() != null) {
            Object details = context.getAuthentication().getDetails();
            if (details instanceof UserDetails userDetails) {
                RequestContext.setUser(userDetails);
            }
        }

        // 7. 【零改动动态扩展】：遍历所有 HTTP 请求头，将自定义或扩展 Header 自动动态装载进 attribute Map 中
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                // 过滤掉已被核心字段占用的已知标头
                if (!isKnownHeader(name)) {
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
     *
     * @param headers 标头映射 Map
     */
    public static void init(Map<String, ?> headers) {
        RequestContext.clean();
        if (ObjectUtils.isEmpty(headers)) {
            RequestContext.setTraceId(UUID.randomUUID().toString());
            return;
        }

        // 1. 解析并设置 TraceId
        String traceId = getHeaderString(headers, HeaderConstant.TRACE_ID);
        RequestContext.setTraceId(StringUtils.hasText(traceId) ? traceId.trim() : UUID.randomUUID().toString());

        // 2. 解析并设置租户编码
        String tenantCodeStr = getHeaderString(headers, HeaderConstant.TENANT_CODE);
        if (StringUtils.hasText(tenantCodeStr)) {
            try {
                RequestContext.setTenantCode(Long.valueOf(tenantCodeStr.trim()));
            } catch (NumberFormatException ignored) {
            }
        }

        // 3. 全量解析并设置其它核心标头
        RequestContext.setToken(getHeaderString(headers, HeaderConstant.AUTHORIZATION));
        RequestContext.setLanguage(getHeaderString(headers, HeaderConstant.ACCEPT_LANGUAGE));
        RequestContext.setApplicationName(getHeaderString(headers, HeaderConstant.APPLICATION_NAME));
        RequestContext.setApplicationVersion(getHeaderString(headers, HeaderConstant.APPLICATION_VERSION));
        RequestContext.setDeviceFingerprint(getHeaderString(headers, HeaderConstant.DEVICE_FINGERPRINT));
        RequestContext.setDeviceId(getHeaderString(headers, HeaderConstant.DEVICE_ID));
        RequestContext.setUserAgent(getHeaderString(headers, HeaderConstant.USER_AGENT));

        // 4. 【零改动动态扩展】：将其它所有未明确定义的标头全量装载进 attribute 动态属性 Map 中
        headers.forEach((k, v) -> {
            if (k != null && !isKnownHeader(k) && v != null) {
                RequestContext.setAttribute(k, v);
            }
        });
    }

    /**
     * 导出当前 RequestContext 中的全量及动态扩展 Header 标头，用于 MQ 发送、RPC 远程调用、线程池透传
     *
     * @return 包含全量核心 Header 及动态扩展 Header 的 Map
     */
    public static Map<String, Object> exportHeaders() {
        Map<String, Object> headers = new HashMap<>();

        // 1. 导出核心字段
        if (StringUtils.hasText(RequestContext.getTraceId())) {
            headers.put(HeaderConstant.TRACE_ID, RequestContext.getTraceId());
        }
        if (RequestContext.getTenantCode() != null) {
            headers.put(HeaderConstant.TENANT_CODE, RequestContext.getTenantCode());
        }
        if (StringUtils.hasText(RequestContext.getToken())) {
            headers.put(HeaderConstant.AUTHORIZATION, RequestContext.getToken());
        }
        if (StringUtils.hasText(RequestContext.getLanguage())) {
            headers.put(HeaderConstant.ACCEPT_LANGUAGE, RequestContext.getLanguage());
        }
        if (StringUtils.hasText(RequestContext.getApplicationName())) {
            headers.put(HeaderConstant.APPLICATION_NAME, RequestContext.getApplicationName());
        }
        if (StringUtils.hasText(RequestContext.getApplicationVersion())) {
            headers.put(HeaderConstant.APPLICATION_VERSION, RequestContext.getApplicationVersion());
        }
        if (StringUtils.hasText(RequestContext.getDeviceFingerprint())) {
            headers.put(HeaderConstant.DEVICE_FINGERPRINT, RequestContext.getDeviceFingerprint());
        }
        if (StringUtils.hasText(RequestContext.getDeviceId())) {
            headers.put(HeaderConstant.DEVICE_ID, RequestContext.getDeviceId());
        }
        if (StringUtils.hasText(RequestContext.getUserAgent())) {
            headers.put(HeaderConstant.USER_AGENT, RequestContext.getUserAgent());
        }

        // 2. 导出动态 attribute 属性扩展字段
        Map<String, Object> attributeMap = RequestContext.getAttribute();
        if (!ObjectUtils.isEmpty(attributeMap)) {
            headers.putAll(attributeMap);
        }

        return headers;
    }

    private static String getHeaderString(Map<String, ?> headers, String key) {
        Object val = headers.get(key);
        return val != null ? String.valueOf(val) : null;
    }

    /**
     * 判断标头名称是否为已知的核心标头
     */
    private static boolean isKnownHeader(String headerName) {
        if (!StringUtils.hasText(headerName)) {
            return false;
        }
        return headerName.equalsIgnoreCase(HeaderConstant.TRACE_ID) ||
               headerName.equalsIgnoreCase(HeaderConstant.TENANT_CODE) ||
               headerName.equalsIgnoreCase(HeaderConstant.AUTHORIZATION) ||
               headerName.equalsIgnoreCase(HeaderConstant.APPLICATION_NAME) ||
               headerName.equalsIgnoreCase(HeaderConstant.APPLICATION_VERSION) ||
               headerName.equalsIgnoreCase(HeaderConstant.USER_AGENT) ||
               headerName.equalsIgnoreCase(HeaderConstant.ACCEPT_LANGUAGE) ||
               headerName.equalsIgnoreCase(HeaderConstant.DEVICE_FINGERPRINT) ||
               headerName.equalsIgnoreCase(HeaderConstant.DEVICE_ID);
    }
}
