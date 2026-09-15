package org.etd.framework.starter.event.server.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.etd.framework.event.core.model.EventMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Starter 持久化监听器委派与异常传播测试。
 */
class EventPersistenceListenerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldPersistEveryEventBeforeOtherListeners() throws Exception {
        EventMessage[] persisted = new EventMessage[1];
        EventPersistenceListener listener = new EventPersistenceListener(message -> persisted[0] = message);
        EventMessage message = createMessage();

        listener.onEvent(message);

        assertThat(listener.eventTypes()).containsExactly("*");
        assertThat(listener.getOrder()).isEqualTo(Integer.MIN_VALUE);
        assertThat(persisted[0]).isSameAs(message);
    }

    @Test
    void shouldPropagatePersistenceFailure() {
        EventPersistenceListener listener = new EventPersistenceListener(message -> {
            throw new IllegalStateException("数据库不可用");
        });

        assertThatThrownBy(() -> listener.onEvent(createMessage()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("数据库不可用");
    }

    private EventMessage createMessage() {
        return new EventMessage("event-1", "upms.user.created", 1,
                Instant.parse("2026-09-15T01:00:00Z"), "upms", "user-1",
                Map.of(), objectMapper.valueToTree(Map.of("userId", 1L)));
    }
}
