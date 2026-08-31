package org.etd.framework.common.core.context.model;

import com.google.common.collect.Maps;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.etd.framework.common.core.user.UserDetails;

import java.io.Serializable;
import java.util.Map;

/**
 * 请求上下文数据模型
 *
 * @author 牛昌
 */
@EqualsAndHashCode
@ToString
@Data
public class RequestContextModel implements Serializable {

    private static final long serialVersionUID = -1L;

    /**
     * 链路追踪 ID
     */
    private String traceId;

    /**
     * 请求客户端 IP
     */
    private String requestIP;

    /**
     * 租户 CODE
     */
    private Long tenantCode;

    /**
     * 认证 Token
     */
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
     * 当前登录用户信息
     */
    private UserDetails userDetails;

    /**
     * 是否忽略租户化查询
     */
    private Boolean ignoreTenant = false;

    /**
     * 扩展自定义属性
     */
    private Map<String, Object> attribute = Maps.newLinkedHashMap();

    public void clean() {
        this.traceId = null;
        this.requestIP = null;
        this.tenantCode = null;
        this.token = null;
        this.language = null;
        this.applicationName = null;
        this.applicationVersion = null;
        this.deviceFingerprint = null;
        this.deviceId = null;
        this.userAgent = null;
        this.userDetails = null;
        this.ignoreTenant = false;
        if (this.attribute != null) {
            this.attribute.clear();
        }
    }

    public Object getAttribute(String key) {
        return attribute != null ? attribute.get(key) : null;
    }

    public void setAttribute(String key, Object value) {
        if (this.attribute == null) {
            this.attribute = Maps.newLinkedHashMap();
        }
        attribute.put(key, value);
    }
}
