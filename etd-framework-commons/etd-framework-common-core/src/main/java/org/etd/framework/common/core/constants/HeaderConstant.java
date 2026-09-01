package org.etd.framework.common.core.constants;

import java.util.List;

/**
 * 全局 HTTP 及分布式协议消息头 (Header) 常量定义
 *
 * @author Young
 * @date 2026/08/31
 */
public final class HeaderConstant {

    private HeaderConstant() {
    }

    /**
     * 允许进入请求上下文并跨服务透传的扩展 Header 前缀
     */
    public static final String EXTENSION_HEADER_PREFIX = "x-";

    /**
     * 链路追踪 ID Header
     */
    public static final String TRACE_ID = "trace-id";

    /**
     * 租户 CODE Header
     */
    public static final String TENANT_CODE = "tenant-code";

    /**
     * 身份认证 Token Header
     */
    public static final String AUTHORIZATION = "Authorization";

    /**
     * 发起请求的应用名称 Header
     */
    public static final String APPLICATION_NAME = "x-application";

    /**
     * 发起请求的应用版本号 Header
     */
    public static final String APPLICATION_VERSION = "x-version";

    /**
     * 客户端 User-Agent 环境标识 Header
     */
    public static final String USER_AGENT = "User-Agent";

    /**
     * 国际化语言 Header (HTTP 官方标准)
     */
    public static final String ACCEPT_LANGUAGE = "Accept-Language";

    /**
     * 客户端设备指纹 Header
     */
    public static final String DEVICE_FINGERPRINT = "x-device-fingerprint";

    /**
     * 客户端设备唯一标识 ID Header
     */
    public static final String DEVICE_ID = "x-device-id";

    /**
     * 代理转发真实客户端 IP Header
     */
    public static final String X_FORWARDED_FOR = "X-Forwarded-For";

    /**
     * 网关真实客户端 IP Header
     */
    public static final String X_REAL_IP = "X-Real-IP";

    /**
     * 常见代理客户端 IP Header 列表（不可变）
     */
    public static final List<String> IP_HEADERS = List.of(
            X_FORWARDED_FOR,
            X_REAL_IP
    );

    /**
     * 文件下载响应头 Content-Disposition
     */
    public static final String CONTENT_DISPOSITION = "Content-Disposition";

    /**
     * RabbitMQ 延迟队列消息头
     */
    public static final String X_DELAY = "x-delay";
}
