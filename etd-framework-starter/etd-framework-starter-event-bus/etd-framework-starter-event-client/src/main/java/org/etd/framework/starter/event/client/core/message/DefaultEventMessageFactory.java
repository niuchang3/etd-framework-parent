package org.etd.framework.starter.event.client.core.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.etd.framework.common.core.context.RequestContextInitializer;
import org.etd.framework.event.core.id.EventIdGenerator;
import org.etd.framework.event.core.model.EventMessage;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 默认统一事件消息工厂。
 */
public class DefaultEventMessageFactory implements EventMessageFactory {

    private static final String UNKNOWN_SOURCE = "unknown";

    private final EventIdGenerator eventIdGenerator;

    private final ObjectMapper objectMapper;

    private final String source;

    public DefaultEventMessageFactory(EventIdGenerator eventIdGenerator, ObjectMapper objectMapper, String source) {
        this.eventIdGenerator = eventIdGenerator;
        this.objectMapper = objectMapper;
        this.source = StringUtils.hasText(source) ? source : UNKNOWN_SOURCE;
    }

    @Override
    public EventMessage create(String eventType, String partitionKey, Object payload) {
        return create(eventType, EventMessage.INITIAL_VERSION, partitionKey, payload);
    }

    @Override
    public EventMessage create(String eventType, int eventVersion, String partitionKey, Object payload) {
        Assert.hasText(eventType, "事件类型不能为空");
        Assert.isTrue(eventVersion > 0, "事件版本必须大于 0");
        Assert.notNull(payload, "事件主体不能为空");
        return new EventMessage(
                eventIdGenerator.generate(), eventType, eventVersion, Instant.now(), source,
                partitionKey, exportContext(), toJsonNode(payload));
    }

    private Map<String, String> exportContext() {
        Map<String, String> context = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : RequestContextInitializer.exportMessageHeaders().entrySet()) {
            if (entry.getValue() != null) {
                context.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        return context;
    }

    private JsonNode toJsonNode(Object payload) {
        if (payload instanceof JsonNode jsonNode) {
            return jsonNode;
        }
        return objectMapper.valueToTree(payload);
    }
}
