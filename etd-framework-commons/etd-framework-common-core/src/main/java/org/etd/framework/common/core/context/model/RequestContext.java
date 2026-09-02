package org.etd.framework.common.core.context.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.common.core.user.UserDetails;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.util.Map;

/**
 * 请求上下文工具类（门面 Facade 类）
 *
 * @author 牛昌
 */
@EqualsAndHashCode
@Data
public class RequestContext implements Serializable {

    private static final long serialVersionUID = -1L;

    private static final ThreadLocal<RequestContextModel> REQUEST_CONTEXT = ThreadLocal.withInitial(RequestContextModel::new);

    /**
     * 获取上下文组合数据模型
     */
    public static RequestContextModel getRequestContext() {
        return REQUEST_CONTEXT.get();
    }

    /**
     * 获取传输消息头上下文
     */
    public static RequestHeaderContext getHeaderContext() {
        return getRequestContext().getHeaders();
    }

    /**
     * 获取控制指令标志上下文
     */
    public static RequestControlFlags getControlFlags() {
        return getRequestContext().getControlFlags();
    }

    /**
     * 清理上下文内容
     */
    public static void clean() {
        RequestContextModel requestContextModel = getRequestContext();
        if (ObjectUtils.isEmpty(requestContextModel)) {
            return;
        }
        requestContextModel.clean();
        REQUEST_CONTEXT.remove();
    }

    /**
     * 复制上下文内容
     */
    public static RequestContextModel copyRequestContext() {
        RequestContextModel requestContext = getRequestContext();
        if (ObjectUtils.isEmpty(requestContext)) {
            return new RequestContextModel();
        }
        return requestContext.copy();
    }

    // ==================== 传输标头 (Headers) 快捷访问 API ====================

    public static String getTraceId() {
        return getHeaderContext().getTraceId();
    }

    public static void setTraceId(String traceId) {
        getHeaderContext().setTraceId(traceId);
    }

    public static String getRequestIP() {
        return getHeaderContext().getRequestIP();
    }

    public static void setRequestIP(String requestIP) {
        getHeaderContext().setRequestIP(requestIP);
    }

    public static Long getTenantCode() {
        return getHeaderContext().getTenantCode();
    }

    public static void setTenantCode(Long tenantCode) {
        getHeaderContext().setTenantCode(tenantCode);
    }

    public static String getToken() {
        return getHeaderContext().getToken();
    }

    public static void setToken(String token) {
        getHeaderContext().setToken(token);
    }

    public static String getLanguage() {
        return getHeaderContext().getLanguage();
    }

    public static void setLanguage(String language) {
        getHeaderContext().setLanguage(language);
    }

    public static String getApplicationName() {
        return getHeaderContext().getApplicationName();
    }

    public static void setApplicationName(String applicationName) {
        getHeaderContext().setApplicationName(applicationName);
    }

    public static String getApplicationVersion() {
        return getHeaderContext().getApplicationVersion();
    }

    public static void setApplicationVersion(String applicationVersion) {
        getHeaderContext().setApplicationVersion(applicationVersion);
    }

    public static String getDeviceFingerprint() {
        return getHeaderContext().getDeviceFingerprint();
    }

    public static void setDeviceFingerprint(String deviceFingerprint) {
        getHeaderContext().setDeviceFingerprint(deviceFingerprint);
    }

    public static String getDeviceId() {
        return getHeaderContext().getDeviceId();
    }

    public static void setDeviceId(String deviceId) {
        getHeaderContext().setDeviceId(deviceId);
    }

    public static String getUserAgent() {
        return getHeaderContext().getUserAgent();
    }

    public static void setUserAgent(String userAgent) {
        getHeaderContext().setUserAgent(userAgent);
    }

    public static Object getAttribute(String key) {
        return getHeaderContext().getAttribute(key);
    }

    public static void setAttribute(String key, Object value) {
        getHeaderContext().setAttribute(key, value);
    }

    public static Map<String, Object> getAttribute() {
        return getHeaderContext().getAttributes();
    }

    public static void setAttribute(Map<String, Object> attribute) {
        getHeaderContext().setAttributes(attribute);
    }

    // ==================== 用户身份 (User) 快捷访问 API ====================

    public static UserDetails getUser() {
        return getRequestContext().getUserDetails();
    }

    public static void setUser(UserDetails userDetails) {
        getRequestContext().setUserDetails(userDetails);
    }

    // ==================== 控制标志 (Flags) 快捷访问 API ====================

    public static boolean getIgnoreTenant() {
        Boolean ignoreTenant = getControlFlags().getIgnoreTenant();
        return Boolean.TRUE.equals(ignoreTenant);
    }

    public static void setIgnoreTenant(Boolean ignore) {
        getControlFlags().setIgnoreTenant(ignore);
    }

    public static boolean getIgnoreDataPermission() {
        Boolean ignoreDataPermission = getControlFlags().getIgnoreDataPermission();
        return Boolean.TRUE.equals(ignoreDataPermission);
    }

    public static void setIgnoreDataPermission(Boolean ignore) {
        getControlFlags().setIgnoreDataPermission(ignore);
    }
}
