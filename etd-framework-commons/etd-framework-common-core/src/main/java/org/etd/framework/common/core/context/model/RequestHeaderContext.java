package org.etd.framework.common.core.context.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.etd.framework.common.core.constants.HeaderConstant;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 传输消息头上下文（协议/网络标头上下文）
 * 专职承载跨 HTTP / MQ / RPC 网关可无损透传的标头数据
 *
 * @author 牛昌
 */
@EqualsAndHashCode
@ToString
@Data
public class RequestHeaderContext implements Serializable {

    private static final long serialVersionUID = -1L;

    /**
     * 链路追踪 ID
     */
    private String traceId;

    /**
     * 租户 CODE
     */
    private Long tenantCode;

    /**
     * 认证 Token
     */
    @ToString.Exclude
    private String token;

    /**
     * 国际化语言标识 (Accept-Language)
     */
    private String language;

    /**
     * 发起请求的应用名称
     */
    private String applicationName;

    /**
     * 发起请求的应用版本号
     */
    private String applicationVersion;

    /**
     * 客户端设备指纹
     */
    private String deviceFingerprint;

    /**
     * 客户端设备 ID
     */
    private String deviceId;

    /**
     * 客户端 User-Agent 字符串
     */
    private String userAgent;

    /**
     * 请求客户端 IP
     */
    private String requestIP;

    /**
     * 扩展自定义属性/标头
     */
    private Map<String, Object> attributes = new LinkedHashMap<>();

    /**
     * 清理所有消息头与动态属性
     */
    public void clean() {
        this.traceId = null;
        this.tenantCode = null;
        this.token = null;
        this.language = null;
        this.applicationName = null;
        this.applicationVersion = null;
        this.deviceFingerprint = null;
        this.deviceId = null;
        this.userAgent = null;
        this.requestIP = null;
        if (this.attributes != null) {
            this.attributes.clear();
        }
    }

    /**
     * 将当前 Header 上下文导出为通用的 Map 结构（供 MQ、RPC、多线程无损透传）
     *
     * @return 包含全量强类型标头及动态 attributes 的 Map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        if (!ObjectUtils.isEmpty(attributes)) {
            attributes.forEach((key, value) -> {
                if (isExtensionHeader(key) && !isKnownHeader(key) && value != null) {
                    map.put(key, value);
                }
            });
        }

        if (StringUtils.hasText(traceId)) {
            map.put(HeaderConstant.TRACE_ID, traceId);
        }
        if (tenantCode != null) {
            map.put(HeaderConstant.TENANT_CODE, tenantCode);
        }
        if (StringUtils.hasText(token)) {
            map.put(HeaderConstant.AUTHORIZATION, token);
        }
        if (StringUtils.hasText(language)) {
            map.put(HeaderConstant.ACCEPT_LANGUAGE, language);
        }
        if (StringUtils.hasText(applicationName)) {
            map.put(HeaderConstant.APPLICATION_NAME, applicationName);
        }
        if (StringUtils.hasText(applicationVersion)) {
            map.put(HeaderConstant.APPLICATION_VERSION, applicationVersion);
        }
        if (StringUtils.hasText(deviceFingerprint)) {
            map.put(HeaderConstant.DEVICE_FINGERPRINT, deviceFingerprint);
        }
        if (StringUtils.hasText(deviceId)) {
            map.put(HeaderConstant.DEVICE_ID, deviceId);
        }
        if (StringUtils.hasText(userAgent)) {
            map.put(HeaderConstant.USER_AGENT, userAgent);
        }
        if (StringUtils.hasText(requestIP)) {
            map.put(HeaderConstant.X_REAL_IP, requestIP);
        }

        return map;
    }

    /**
     * 创建独立副本，避免异步线程共享可变 attributes。
     */
    public RequestHeaderContext copy() {
        RequestHeaderContext copy = new RequestHeaderContext();
        copy.traceId = traceId;
        copy.tenantCode = tenantCode;
        copy.token = token;
        copy.language = language;
        copy.applicationName = applicationName;
        copy.applicationVersion = applicationVersion;
        copy.deviceFingerprint = deviceFingerprint;
        copy.deviceId = deviceId;
        copy.userAgent = userAgent;
        copy.requestIP = requestIP;
        copy.attributes = attributes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(attributes);
        return copy;
    }

    /**
     * 从 Map 结构还原 Header 上下文（支持大小写不敏感匹配）
     *
     * @param map 输入的 Map 结构标头
     */
    public void fromMap(Map<String, ?> map) {
        clean();
        if (ObjectUtils.isEmpty(map)) {
            return;
        }

        this.traceId = getHeaderString(map, HeaderConstant.TRACE_ID);
        String tenantCodeStr = getHeaderString(map, HeaderConstant.TENANT_CODE);
        if (StringUtils.hasText(tenantCodeStr)) {
            try {
                this.tenantCode = Long.valueOf(tenantCodeStr.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        this.token = getHeaderString(map, HeaderConstant.AUTHORIZATION);
        this.language = getHeaderString(map, HeaderConstant.ACCEPT_LANGUAGE);
        this.applicationName = getHeaderString(map, HeaderConstant.APPLICATION_NAME);
        this.applicationVersion = getHeaderString(map, HeaderConstant.APPLICATION_VERSION);
        this.deviceFingerprint = getHeaderString(map, HeaderConstant.DEVICE_FINGERPRINT);
        this.deviceId = getHeaderString(map, HeaderConstant.DEVICE_ID);
        this.userAgent = getHeaderString(map, HeaderConstant.USER_AGENT);
        this.requestIP = getHeaderString(map, HeaderConstant.X_REAL_IP);

        // 还原扩展 attributes
        map.forEach((k, v) -> {
            if (isExtensionHeader(k) && !isKnownHeader(k) && v != null) {
                setAttribute(k, v);
            }
        });
    }

    /**
     * 获取 Attribute 属性值
     *
     * @param key 参数 key
     * @return 处理结果
     */
    public Object getAttribute(String key) {
        return attributes != null ? attributes.get(key) : null;
    }

    /**
     * 设置 Attribute 属性值
     *
     * @param key 参数 key
     * @param value 参数 value
     */
    public void setAttribute(String key, Object value) {
        if (this.attributes == null) {
            this.attributes = new LinkedHashMap<>();
        }
        this.attributes.put(key, value);
    }

    /**
     * 大小写不敏感的 Map 查找
     */
    private static String getHeaderString(Map<String, ?> map, String key) {
        if (map == null || key == null) {
            return null;
        }
        Object val = map.get(key);
        if (val != null) {
            return String.valueOf(val);
        }
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            if (key.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue() != null ? String.valueOf(entry.getValue()) : null;
            }
        }
        return null;
    }

    /**
     * 判断 KnownHeader 状态
     *
     * @param headerName 参数 headerName
     * @return 处理结果
     */
    public static boolean isKnownHeader(String headerName) {
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
               headerName.equalsIgnoreCase(HeaderConstant.DEVICE_ID) ||
               headerName.equalsIgnoreCase(HeaderConstant.X_REAL_IP) ||
               headerName.equalsIgnoreCase(HeaderConstant.X_FORWARDED_FOR);
    }

    /**
     * 扩展上下文只允许使用 x- 前缀，阻止 Cookie、Origin 等浏览器头进入透传链路。
     */
    public static boolean isExtensionHeader(String headerName) {
        return StringUtils.hasText(headerName)
                && headerName.regionMatches(true, 0, HeaderConstant.EXTENSION_HEADER_PREFIX, 0,
                HeaderConstant.EXTENSION_HEADER_PREFIX.length());
    }
}
