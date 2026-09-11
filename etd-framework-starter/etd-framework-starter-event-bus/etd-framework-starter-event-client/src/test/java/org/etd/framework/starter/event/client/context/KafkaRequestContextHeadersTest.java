package org.etd.framework.starter.event.client.context;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.etd.framework.common.core.constants.HeaderConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kafka 请求上下文消息头转换测试。
 */
class KafkaRequestContextHeadersTest {

    @AfterEach
    void cleanRequestContext() {
        RequestContext.clean();
    }

    @Test
    void shouldWriteSafeRequestContextToKafkaHeaders() {
        RequestContext.setTraceId("trace-001");
        RequestContext.setTenantCode(1001L);
        RequestContext.setToken("secret-token");
        RecordHeaders headers = new RecordHeaders();

        KafkaRequestContextHeaders.writeTo(headers);

        assertThat(readValue(headers, HeaderConstant.TRACE_ID)).isEqualTo("trace-001");
        assertThat(readValue(headers, HeaderConstant.TENANT_CODE)).isEqualTo("1001");
        assertThat(headers.lastHeader(HeaderConstant.AUTHORIZATION)).isNull();
    }

    @Test
    void shouldReadKafkaHeadersAsRequestContextMap() {
        RecordHeaders headers = new RecordHeaders();
        headers.add(HeaderConstant.TRACE_ID, "trace-002".getBytes(StandardCharsets.UTF_8));
        headers.add(HeaderConstant.TENANT_CODE, "1002".getBytes(StandardCharsets.UTF_8));

        Map<String, Object> values = KafkaRequestContextHeaders.readFrom(headers);

        assertThat(values).containsEntry(HeaderConstant.TRACE_ID, "trace-002")
                .containsEntry(HeaderConstant.TENANT_CODE, "1002");
    }

    @Test
    void shouldPropagateAndCleanRequestContextAroundKafkaConsumption() {
        RequestContext.setTraceId("trace-003");
        ProducerRecord<Object, Object> producerRecord = new ProducerRecord<>("etd.event.bus", "event");
        new KafkaRequestContextProducerInterceptor().onSend(producerRecord);
        RequestContext.clean();
        ConsumerRecord<Object, Object> consumerRecord = new ConsumerRecord<>(
                "etd.event.bus", 0, 0L, null, "event");
        producerRecord.headers().forEach(header -> consumerRecord.headers().add(header));
        KafkaRequestContextRecordInterceptor interceptor = new KafkaRequestContextRecordInterceptor();

        interceptor.intercept(consumerRecord, null);
        assertThat(RequestContext.getTraceId()).isEqualTo("trace-003");

        interceptor.afterRecord(consumerRecord, null);
        assertThat(RequestContext.getTraceId()).isNull();
    }

    private String readValue(RecordHeaders headers, String name) {
        return new String(headers.lastHeader(name).value(), StandardCharsets.UTF_8);
    }
}
