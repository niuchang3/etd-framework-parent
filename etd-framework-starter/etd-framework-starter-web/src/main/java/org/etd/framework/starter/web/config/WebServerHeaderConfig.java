package org.etd.framework.starter.web.config;

import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.util.unit.DataSize;

/** 为内嵌 Tomcat 提供统一请求头上限，应用显式配置时沿用 Spring Boot 的绑定结果。 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(TomcatServletWebServerFactory.class)
public class WebServerHeaderConfig {

    /** 框架默认允许的请求头大小，单位为字节，包含认证头和 Cookie 等请求头。 */
    private static final int DEFAULT_MAX_REQUEST_HEADER_BYTES = (int) DataSize.ofKilobytes(32).toBytes();

    /** Spring Boot 原生请求头大小配置键，支持 YAML、环境变量及启动参数覆盖。 */
    private static final String MAX_REQUEST_HEADER_SIZE_PROPERTY = "server.max-http-request-header-size";

    /**
     * 未显式配置时，将框架默认值应用于 Tomcat HTTP 连接器。
     *
     * @param environment 已加载应用配置和环境变量的运行环境
     * @return 在 Spring Boot 内置工厂定制器之后执行的请求头定制器
     */
    @Bean
    @ConditionalOnMissingBean(name = "etdRequestHeaderCustomizer")
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> etdRequestHeaderCustomizer(
            Environment environment) {
        return factory -> {
            if (environment.containsProperty(MAX_REQUEST_HEADER_SIZE_PROPERTY)) {
                return;
            }
            // 普通工厂定制器晚于 Boot 的 Ordered 定制器执行，连接器定制按添加顺序生效。
            TomcatConnectorCustomizer connectorCustomizer = connector -> {
                if (connector.getProtocolHandler() instanceof AbstractHttp11Protocol<?> protocol) {
                    protocol.setMaxHttpRequestHeaderSize(DEFAULT_MAX_REQUEST_HEADER_BYTES);
                }
            };
            factory.addConnectorCustomizers(connectorCustomizer);
        };
    }
}
