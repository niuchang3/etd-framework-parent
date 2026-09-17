package org.etd.framework.common.core.context;

import jakarta.servlet.http.HttpServletRequest;
import org.etd.framework.common.core.constants.HeaderConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.context.model.RequestControlFlags;
import org.etd.framework.common.core.context.model.RequestHeaderContext;
import org.etd.framework.common.core.user.PermissionAuthority;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.common.utils.ip.IpUtil;
import org.etd.framework.common.utils.json.JsonUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.Enumeration;
import java.util.LinkedHashSet;
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

        // 2. 解析并设置 TraceId（优先 SkyWalking TraceContext -> 其次 HTTP Header -> 最后 UUID 补全）
        String traceId = null;
        try {
            Class<?> clazz = Class.forName("org.apache.skywalking.apm.toolkit.trace.TraceContext");
            java.lang.reflect.Method method = clazz.getMethod("traceId");
            Object result = method.invoke(null);
            if (result instanceof String swTraceId && StringUtils.hasText(swTraceId)
                    && !"IgnoredTraced".equalsIgnoreCase(swTraceId) && !"N/A".equalsIgnoreCase(swTraceId)) {
                traceId = swTraceId;
            }
        } catch (Throwable ignored) {
        }
        if (!StringUtils.hasText(traceId)) {
            traceId = request.getHeader(HeaderConstant.TRACE_ID);
        }
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
     * Map 标头与序列化数据的全量上下文初始化与还原
     * 支持包含 headers、userDetails 及 controlFlags 的完整上下文还原
     *
     * @param headers 包含上下文信息的 Map
     */
    public static void init(Map<String, ?> headers) {
        RequestContext.clean();
        if (ObjectUtils.isEmpty(headers)) {
            RequestContext.setTraceId(UUID.randomUUID().toString());
            return;
        }

        // 1. 调用 RequestHeaderContext 的无损还原与大小写兼容能力
        RequestHeaderContext headerContext = RequestContext.getHeaderContext();
        headerContext.fromMap(headers);

        // 2. 还原安全与用户身份领域模型 (UserDetails)
        Object userDetailsObj = getHeaderValue(headers, HeaderConstant.InternalHeader.X_USER_DETAILS);
        if (userDetailsObj instanceof UserDetails userDetails) {
            RequestContext.setUser(userDetails);
        } else if (userDetailsObj != null) {
            String userJson = userDetailsObj instanceof String str ? str : JsonUtils.toJson(userDetailsObj);
            if (StringUtils.hasText(userJson)) {
                UserDetails deserializedUser = JsonUtils.fromJson(userJson, UserDetails.class);
                if (deserializedUser != null) {
                    RequestContext.setUser(deserializedUser);
                }
            }
        }

        // 3. 如果传入标头中缺乏 traceId，进行 UUID 兜底补全
        if (!StringUtils.hasText(headerContext.getTraceId())) {
            headerContext.setTraceId(UUID.randomUUID().toString());
        }
    }

    /**
     * 导出 RequestContextModel 的全量上下文为 Map 结构。
     * 包含传输标头 (Headers，自动去除敏感 Token) 以及安全用户模型 (UserDetails，去敏感密码)。
     *
     * @return 包含全量 RequestContextModel 的 Map 结构
     */
    public static Map<String, Object> exportMessageHeaders() {
        // 1. 导出消息头上下文
        Map<String, Object> map = RequestContext.getHeaderContext().toMap();
        map.keySet().removeIf(key -> HeaderConstant.AUTHORIZATION.equalsIgnoreCase(key));

        // 2. 导出安全与用户身份领域模型（安全去敏感密码）
        UserDetails userDetails = RequestContext.getUser();
        if (userDetails != null) {
            UserDetails safeUserDetails = copySafeUserDetails(userDetails);
            String userJson = JsonUtils.toJson(safeUserDetails);
            if (StringUtils.hasText(userJson)) {
                map.put(HeaderConstant.InternalHeader.X_USER_DETAILS, userJson);
            }
        }

        return map;
    }

    /**
     * 大小写不敏感地从 Map 中提取属性值
     */
    private static Object getHeaderValue(Map<String, ?> map, String key) {
        if (map == null || key == null) {
            return null;
        }
        Object val = map.get(key);
        if (val != null) {
            return val;
        }
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            if (key.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 复制安全的 UserDetails 副本（清空密码敏感字段）
     */
    private static UserDetails copySafeUserDetails(UserDetails source) {
        if (source == null) {
            return null;
        }
        UserDetails copy = new UserDetails();
        BeanUtils.copyProperties(source, copy);
        copy.setPassword(null);
        if (source.getRoleCodes() != null) {
            copy.setRoleCodes(new LinkedHashSet<>(source.getRoleCodes()));
        }
        if (source.getAuthorities() != null) {
            copy.setAuthorities(source.getAuthorities().stream()
                    .map(authority -> new PermissionAuthority(authority.getAuthority()))
                    .toList());
        }
        return copy;
    }
}
