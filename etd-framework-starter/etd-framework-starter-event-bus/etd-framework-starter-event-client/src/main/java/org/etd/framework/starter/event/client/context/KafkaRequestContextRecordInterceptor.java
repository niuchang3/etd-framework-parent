package org.etd.framework.starter.event.client.context;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.etd.framework.common.core.context.RequestContextInitializer;
import org.etd.framework.common.core.context.model.RequestContext;
import org.slf4j.MDC;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.util.StringUtils;

/**
 * 在消费单条 Kafka 事件前恢复请求上下文，并在处理结束后清理线程状态。
 */
public class KafkaRequestContextRecordInterceptor implements RecordInterceptor<Object, Object> {

    @Override
    public ConsumerRecord<Object, Object> intercept(ConsumerRecord<Object, Object> record,
                                                     Consumer<Object, Object> consumer) {
        RequestContextInitializer.init(KafkaRequestContextHeaders.readFrom(record.headers()));
        if (StringUtils.hasText(RequestContext.getTraceId())) {
            MDC.put("traceId", RequestContext.getTraceId());
        }
        return record;
    }

    @Override
    public void afterRecord(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
        clearRequestContext();
    }

    @Override
    public void clearThreadState(Consumer<?, ?> consumer) {
        clearRequestContext();
    }

    private void clearRequestContext() {
        RequestContext.clean();
        MDC.clear();
    }
}
