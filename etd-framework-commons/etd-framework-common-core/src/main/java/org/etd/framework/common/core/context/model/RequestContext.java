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

    /**
     * 获取 TraceId 属性值
     *
     * @return 处理结果
     */
    public static String getTraceId() {
        return getHeaderContext().getTraceId();
    }

    /**
     * 设置 TraceId 属性值
     *
     * @param traceId 参数 traceId
     * @return 处理结果
     */
    public static void setTraceId(String traceId) {
        getHeaderContext().setTraceId(traceId);
    }

    /**
     * 获取 RequestIP 属性值
     *
     * @return 处理结果
     */
    public static String getRequestIP() {
        return getHeaderContext().getRequestIP();
    }

    /**
     * 设置 RequestIP 属性值
     *
     * @param requestIP 参数 requestIP
     * @return 处理结果
     */
    public static void setRequestIP(String requestIP) {
        getHeaderContext().setRequestIP(requestIP);
    }

    /**
     * 获取 TenantCode 属性值
     *
     * @return 处理结果
     */
    public static Long getTenantCode() {
        return getHeaderContext().getTenantCode();
    }

    /**
     * 设置 TenantCode 属性值
     *
     * @param tenantCode 参数 tenantCode
     * @return 处理结果
     */
    public static void setTenantCode(Long tenantCode) {
        getHeaderContext().setTenantCode(tenantCode);
    }

    /**
     * 获取 Token 属性值
     *
     * @return 处理结果
     */
    public static String getToken() {
        return getHeaderContext().getToken();
    }

    /**
     * 设置 Token 属性值
     *
     * @param token 参数 token
     * @return 处理结果
     */
    public static void setToken(String token) {
        getHeaderContext().setToken(token);
    }

    /**
     * 获取 Language 属性值
     *
     * @return 处理结果
     */
    public static String getLanguage() {
        return getHeaderContext().getLanguage();
    }

    /**
     * 设置 Language 属性值
     *
     * @param language 参数 language
     * @return 处理结果
     */
    public static void setLanguage(String language) {
        getHeaderContext().setLanguage(language);
    }

    /**
     * 获取 ApplicationName 属性值
     *
     * @return 处理结果
     */
    public static String getApplicationName() {
        return getHeaderContext().getApplicationName();
    }

    /**
     * 设置 ApplicationName 属性值
     *
     * @param applicationName 参数 applicationName
     * @return 处理结果
     */
    public static void setApplicationName(String applicationName) {
        getHeaderContext().setApplicationName(applicationName);
    }

    /**
     * 获取 ApplicationVersion 属性值
     *
     * @return 处理结果
     */
    public static String getApplicationVersion() {
        return getHeaderContext().getApplicationVersion();
    }

    /**
     * 设置 ApplicationVersion 属性值
     *
     * @param applicationVersion 参数 applicationVersion
     * @return 处理结果
     */
    public static void setApplicationVersion(String applicationVersion) {
        getHeaderContext().setApplicationVersion(applicationVersion);
    }

    /**
     * 获取 DeviceFingerprint 属性值
     *
     * @return 处理结果
     */
    public static String getDeviceFingerprint() {
        return getHeaderContext().getDeviceFingerprint();
    }

    /**
     * 设置 DeviceFingerprint 属性值
     *
     * @param deviceFingerprint 参数 deviceFingerprint
     * @return 处理结果
     */
    public static void setDeviceFingerprint(String deviceFingerprint) {
        getHeaderContext().setDeviceFingerprint(deviceFingerprint);
    }

    /**
     * 获取 DeviceId 属性值
     *
     * @return 处理结果
     */
    public static String getDeviceId() {
        return getHeaderContext().getDeviceId();
    }

    /**
     * 设置 DeviceId 属性值
     *
     * @param deviceId 参数 deviceId
     * @return 处理结果
     */
    public static void setDeviceId(String deviceId) {
        getHeaderContext().setDeviceId(deviceId);
    }

    /**
     * 获取 UserAgent 属性值
     *
     * @return 处理结果
     */
    public static String getUserAgent() {
        return getHeaderContext().getUserAgent();
    }

    /**
     * 设置 UserAgent 属性值
     *
     * @param userAgent 参数 userAgent
     * @return 处理结果
     */
    public static void setUserAgent(String userAgent) {
        getHeaderContext().setUserAgent(userAgent);
    }

    /**
     * 获取 Attribute 属性值
     *
     * @param key 参数 key
     * @return 处理结果
     */
    public static Object getAttribute(String key) {
        return getHeaderContext().getAttribute(key);
    }

    /**
     * 设置 Attribute 属性值
     *
     * @param key 参数 key
     * @param value 参数 value
     * @return 处理结果
     */
    public static void setAttribute(String key, Object value) {
        getHeaderContext().setAttribute(key, value);
    }

    /**
     * 获取 Attribute 属性值
     *
     * @return 处理结果
     */
    public static Map<String, Object> getAttribute() {
        return getHeaderContext().getAttributes();
    }

    /**
     * 设置 Attribute 属性值
     *
     * @param Map<String 参数 Map<String
     * @param attribute 参数 attribute
     * @return 处理结果
     */
    public static void setAttribute(Map<String, Object> attribute) {
        getHeaderContext().setAttributes(attribute);
    }

    // ==================== 用户身份 (User) 快捷访问 API ====================

    /**
     * 获取 User 属性值
     *
     * @return 处理结果
     */
    public static UserDetails getUser() {
        return getRequestContext().getUserDetails();
    }

    /**
     * 设置 User 属性值
     *
     * @param userDetails 参数 userDetails
     * @return 处理结果
     */
    public static void setUser(UserDetails userDetails) {
        getRequestContext().setUserDetails(userDetails);
    }

    // ==================== 控制标志 (Flags) 快捷访问 API ====================

    /**
     * 获取 IgnoreTenant 属性值
     *
     * @return 处理结果
     */
    public static boolean getIgnoreTenant() {
        Boolean ignoreTenant = getControlFlags().getIgnoreTenant();
        return Boolean.TRUE.equals(ignoreTenant);
    }

    /**
     * 设置 IgnoreTenant 属性值
     *
     * @param ignore 参数 ignore
     * @return 处理结果
     */
    public static void setIgnoreTenant(Boolean ignore) {
        getControlFlags().setIgnoreTenant(ignore);
    }

    /**
     * 获取 IgnoreDataPermission 属性值
     *
     * @return 处理结果
     */
    public static boolean getIgnoreDataPermission() {
        Boolean ignoreDataPermission = getControlFlags().getIgnoreDataPermission();
        return Boolean.TRUE.equals(ignoreDataPermission);
    }

    /**
     * 设置 IgnoreDataPermission 属性值
     *
     * @param ignore 参数 ignore
     * @return 处理结果
     */
    public static void setIgnoreDataPermission(Boolean ignore) {
        getControlFlags().setIgnoreDataPermission(ignore);
    }
}
