package org.etd.framework.starter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/** 同时加载Boot默认Jackson配置，验证框架规则在实际自动配置顺序下生效。 */
class WebJacksonAutoConfigurationTest {
    /** Long统一为字符串，避免默认ObjectMapper提前注册导致框架配置退让。 */
    @Test
    void shouldSerializeLongWithBootJacksonAutoConfiguration() {
        var configurations = AutoConfigurations.of(JacksonAutoConfiguration.class, WebJacksonConfig.class);
        var runner = new ApplicationContextRunner().withConfiguration(configurations);
        runner.run(context -> {
            assertThat(context).hasSingleBean(ObjectMapper.class);
            var mapper = context.getBean(ObjectMapper.class);
            assertThat(mapper.writeValueAsString(9007199254740993L)).isEqualTo("\"9007199254740993\"");
        });
    }
}
