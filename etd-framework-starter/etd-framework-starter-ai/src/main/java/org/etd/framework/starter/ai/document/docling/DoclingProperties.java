package org.etd.framework.starter.ai.document.docling;

import java.net.URI;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/** Docling 服务连接配置；默认不启用，不提供服务地址或凭证默认值。 */
@Getter
@Setter
@ConfigurationProperties(DoclingProperties.PREFIX)
public class DoclingProperties {
    /** 连接配置绑定前缀，同时用于自动配置启用条件。 */
    public static final String PREFIX = "etd.ai.document.docling";
    /** 显式启用属性名，与 enabled 属性绑定保持一致。 */
    public static final String ENABLED_PROPERTY = "enabled";
    /** 默认建立连接超时，单位由 Duration 表达。 */
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    /** 默认单次 HTTP 请求超时，不代表整个任务处理时长。 */
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(60);
    /** 默认原文件上限 20 MiB，用于约束 SDK Base64 传输的内存占用。 */
    private static final int DEFAULT_MAX_FILE_BYTES = 20 * 1024 * 1024;
    /** 可接受的服务协议，不允许文件或其他 URI 协议。 */
    private static final String HTTP_SCHEME = "http";
    /** 可接受的加密 HTTP 服务协议。 */
    private static final String HTTPS_SCHEME = "https";

    /** 显式启用 Docling 解析，默认关闭。 */
    private boolean enabled;
    /** 服务根地址，启用时必填，不能包含用户信息、查询参数或片段。 */
    private URI baseUrl;
    /** 服务 API Key，由环境变量注入；空值不发送认证头。 */
    private String apiKey;
    /** 建立连接的超时，默认 10 秒。 */
    private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    /** 每次 HTTP 请求的超时，默认 60 秒，不等于整个解析任务时长。 */
    private Duration readTimeout = DEFAULT_READ_TIMEOUT;
    /** 原文件最大字节数，默认 20 MiB；SDK 使用 Base64 传输，必须限制单次内存占用。 */
    private int maxFileBytes = DEFAULT_MAX_FILE_BYTES;

    /**
     * 启用时检查配置；不连接远端，不要求服务在应用启动时在线。
     * @throws IllegalArgumentException 地址、超时或大小配置无效
     */
    public void validate() {
        Assert.notNull(baseUrl, "启用文档解析服务时必须配置base-url");
        Assert.isTrue((HTTP_SCHEME.equals(baseUrl.getScheme()) || HTTPS_SCHEME.equals(baseUrl.getScheme()))
                && baseUrl.getHost() != null && baseUrl.getRawUserInfo() == null
                && baseUrl.getRawQuery() == null && baseUrl.getRawFragment() == null,
                "文档解析服务必须使用无凭证、查询参数和片段的HTTP地址");
        requirePositive(connectTimeout, "连接超时");
        requirePositive(readTimeout, "请求超时");
        Assert.isTrue(maxFileBytes > 0 && maxFileBytes < Integer.MAX_VALUE, "文件大小限制无效");
    }

    private void requirePositive(Duration duration, String name) {
        Assert.notNull(duration, name + "不能为空");
        Assert.isTrue(duration.compareTo(Duration.ZERO) > 0, name + "必须大于零");
    }
}
