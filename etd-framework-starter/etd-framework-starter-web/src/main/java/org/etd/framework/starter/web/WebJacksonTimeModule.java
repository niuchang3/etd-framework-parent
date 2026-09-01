package org.etd.framework.starter.web;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.Function;

/**
 * Web 接口时间序列化规范。
 * <p>
 * 时间点统一转换到配置时区并携带偏移量；无时区的 LocalDateTime 按配置时区解释。
 */
final class WebJacksonTimeModule {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSXXX");

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private WebJacksonTimeModule() {
    }

    static JavaTimeModule create(ZoneId zoneId) {
        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(Instant.class, instantSerializer(zoneId, Function.identity()));
        module.addSerializer(OffsetDateTime.class, instantSerializer(zoneId, OffsetDateTime::toInstant));
        module.addSerializer(ZonedDateTime.class, instantSerializer(zoneId, ZonedDateTime::toInstant));
        module.addSerializer(LocalDateTime.class, localDateTimeSerializer(zoneId));
        module.addDeserializer(LocalDateTime.class, localDateTimeDeserializer(zoneId));
        module.addSerializer(LocalDate.class, new LocalDateSerializer(DATE_FORMATTER));
        module.addDeserializer(LocalDate.class, localDateDeserializer(zoneId));
        module.addSerializer(LocalTime.class, new LocalTimeSerializer(TIME_FORMATTER));
        module.addDeserializer(LocalTime.class, new LocalTimeDeserializer(TIME_FORMATTER));
        return module;
    }

    private static <T> JsonSerializer<T> instantSerializer(ZoneId zoneId, Function<T, Instant> converter) {
        return new JsonSerializer<>() {
            @Override
            public void serialize(T value, JsonGenerator generator, SerializerProvider serializers)
                    throws IOException {
                generator.writeString(DATE_TIME_FORMATTER.format(converter.apply(value).atZone(zoneId)));
            }
        };
    }

    private static JsonSerializer<LocalDateTime> localDateTimeSerializer(ZoneId zoneId) {
        return new JsonSerializer<>() {
            @Override
            public void serialize(LocalDateTime value, JsonGenerator generator, SerializerProvider serializers)
                    throws IOException {
                generator.writeString(DATE_TIME_FORMATTER.format(value.atZone(zoneId)));
            }
        };
    }

    private static JsonDeserializer<LocalDateTime> localDateTimeDeserializer(ZoneId zoneId) {
        return new JsonDeserializer<>() {
            @Override
            public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                String value = parser.getValueAsString();
                try {
                    return OffsetDateTime.parse(value).atZoneSameInstant(zoneId).toLocalDateTime();
                } catch (DateTimeParseException ignored) {
                    // 兼容历史无偏移量输入，并按配置时区解释其本地时间语义。
                    return LocalDateTime.parse(value);
                }
            }
        };
    }

    private static JsonDeserializer<LocalDate> localDateDeserializer(ZoneId zoneId) {
        return new JsonDeserializer<>() {
            @Override
            public LocalDate deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                if (JsonToken.VALUE_NUMBER_INT.equals(parser.currentToken())) {
                    // 兼容历史 JWT 中以毫秒时间戳保存的生日，读取后立即收敛为纯日期。
                    return Instant.ofEpochMilli(parser.getLongValue()).atZone(zoneId).toLocalDate();
                }
                return LocalDate.parse(parser.getValueAsString(), DATE_FORMATTER);
            }
        };
    }
}
