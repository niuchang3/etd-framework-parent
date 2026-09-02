package org.etd.framework.starter.web;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.math.BigInteger;
import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Web 层 JSON 映射器自动配置
 *
 * @author Young
 */
@AutoConfiguration
public class WebJacksonConfig {

    private static final String JACKSON_TIME_ZONE_PROPERTY = "spring.jackson.time-zone";

    private static final String DEFAULT_TIME_ZONE = "Asia/Shanghai";

    /**
     * 提供全局 JSON 映射器（处理 Long 转 String，防止前端精度丢失）
     *
     * @return JSON 映射器
     */
    /**
     * object Mapper
     *
     * @param environment 参数 environment
     * @return 处理结果
     */
    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper(Environment environment) {
        ZoneId zoneId = ZoneId.of(environment.getProperty(JACKSON_TIME_ZONE_PROPERTY, DEFAULT_TIME_ZONE));
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(BigInteger.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(WebJacksonTimeModule.create(zoneId));
        objectMapper.registerModule(simpleModule);
        configureTimeZone(objectMapper, zoneId);
        // 兼容前端渐进式升级，忽略后端 DTO 尚未声明的请求字段。
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.enable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    private void configureTimeZone(ObjectMapper objectMapper, ZoneId zoneId) {
        TimeZone timeZone = TimeZone.getTimeZone(zoneId);
        objectMapper.setTimeZone(timeZone);
        objectMapper.setDateFormat(new StdDateFormat().withTimeZone(timeZone).withColonInTimeZone(true));
    }
}
