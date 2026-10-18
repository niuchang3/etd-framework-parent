package org.etd.framework.starter.web.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;

/** 核对框架默认值及应用覆盖值在 Boot 定制后的实际 Tomcat 连接器上生效。 */
class WebServerHeaderConfigTest {

    /** 使用真实服务器工厂和 Boot 定制器，避免仅验证配置对象而遗漏执行顺序。 */
    private final WebApplicationContextRunner runner = createRunner();

    /** 无应用配置时，统一使用框架的 32KB 请求头上限。 */
    @Test
    void shouldUseFrameworkDefaultHeaderSize() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            TomcatServletWebServerFactory factory = context.getBean(TomcatServletWebServerFactory.class);
            assertThat(getRequestHeaderBytes(factory)).isEqualTo(DataSize.ofKilobytes(32).toBytes());
        });
    }

    /** 应用既可以收紧上限，也可以扩大上限，框架默认值不能覆盖显式配置。 */
    @ParameterizedTest
    @ValueSource(strings = {"8KB", "64KB"})
    void shouldRespectApplicationHeaderSize(String size) {
        String property = "server.max-http-request-header-size=" + size;
        runner.withPropertyValues(property).run(context -> {
            assertThat(context).hasNotFailed();
            TomcatServletWebServerFactory factory = context.getBean(TomcatServletWebServerFactory.class);
            assertThat(getRequestHeaderBytes(factory)).isEqualTo(DataSize.parse(size).toBytes());
        });
    }

    /** 按生产路径装配工厂、工厂定制后处理器和 Tomcat 原生属性定制器。 */
    private static WebApplicationContextRunner createRunner() {
        AutoConfigurations configurations = AutoConfigurations.of(
                ServletWebServerFactoryAutoConfiguration.class,
                EmbeddedWebServerFactoryCustomizerAutoConfiguration.class,
                WebServerHeaderConfig.class);
        WebApplicationContextRunner runner = new WebApplicationContextRunner();
        return runner.withConfiguration(configurations);
    }

    /** 将所有连接器定制器按实际顺序执行后，读取 HTTP 协议处理器的最终上限。 */
    private int getRequestHeaderBytes(TomcatServletWebServerFactory factory) {
        Connector connector = new Connector();
        for (var customizer : factory.getTomcatConnectorCustomizers()) {
            customizer.customize(connector);
        }
        AbstractHttp11Protocol<?> protocol = (AbstractHttp11Protocol<?>) connector.getProtocolHandler();
        return protocol.getMaxHttpRequestHeaderSize();
    }
}
