package org.etd.framework.starter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

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

    /** 默认按东八区输出所有时间点类型，舍弃显示层的小数秒并移除偏移后缀。 */
    @Test
    void shouldSerializeTimePointsInShanghaiToSeconds() {
        var configurations = AutoConfigurations.of(JacksonAutoConfiguration.class, WebJacksonConfig.class);
        var runner = new ApplicationContextRunner().withConfiguration(configurations);
        runner.run(context -> {
            var mapper = context.getBean(ObjectMapper.class);
            var instant = Instant.parse("2026-10-24T10:48:19.999999999Z");
            var offset = OffsetDateTime.parse("2026-10-24T06:48:19.123-04:00");
            var zoned = instant.atZone(ZoneId.of("UTC"));
            var local = LocalDateTime.of(2026, 10, 24, 18, 48, 19, 123000000);
            var boundaryDate = Date.from(instant);
            List<Object> values = List.of(instant, offset, zoned, local, boundaryDate);
            for (Object value : values) {
                var json = mapper.writeValueAsString(value);
                assertThat(json).isEqualTo("\"2026-10-24 18:48:19\"");
            }
        });
    }

    /** 默认时区转换跨日时仍按真实时间点计算，不依赖 JVM 所在时区。 */
    @Test
    void shouldConvertTimePointsAcrossDateBoundary() {
        var configurations = AutoConfigurations.of(JacksonAutoConfiguration.class, WebJacksonConfig.class);
        var runner = new ApplicationContextRunner().withConfiguration(configurations);
        runner.run(context -> {
            var mapper = context.getBean(ObjectMapper.class);
            var instant = Instant.parse("2026-10-24T18:48:19.123Z");
            var json = mapper.writeValueAsString(instant);
            assertThat(json).isEqualTo("\"2026-10-25 02:48:19\"");
        });
    }

    /** 显式配置时区仍然生效，显示格式不再包含时区后缀。 */
    @Test
    void shouldHonorConfiguredTimeZone() {
        var configurations = AutoConfigurations.of(JacksonAutoConfiguration.class, WebJacksonConfig.class);
        var runner = new ApplicationContextRunner().withConfiguration(configurations)
                .withPropertyValues("spring.jackson.time-zone=UTC");
        runner.run(context -> {
            var mapper = context.getBean(ObjectMapper.class);
            var instant = Instant.parse("2026-10-24T10:48:19.123Z");
            var json = mapper.writeValueAsString(instant);
            assertThat(json).isEqualTo("\"2026-10-24 10:48:19\"");
        });
    }

    /** 纯日期、纯时间及空值维持各自语义，不额外附加日期或时区信息。 */
    @Test
    void shouldKeepDateTimeAndNullFormats() {
        var configurations = AutoConfigurations.of(JacksonAutoConfiguration.class, WebJacksonConfig.class);
        var runner = new ApplicationContextRunner().withConfiguration(configurations);
        runner.run(context -> {
            var mapper = context.getBean(ObjectMapper.class);
            var date = LocalDate.of(2026, 10, 24);
            var time = LocalTime.of(18, 48, 19, 123000000);
            var dateJson = mapper.writeValueAsString(date);
            var timeJson = mapper.writeValueAsString(time);
            var nullJson = mapper.writeValueAsString(null);
            assertThat(dateJson).isEqualTo("\"2026-10-24\"");
            assertThat(timeJson).isEqualTo("\"18:48:19\"");
            assertThat(nullJson).isEqualTo("null");
        });
    }
}
