package org.etd.framework.starter.event.server.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.etd.framework.event.core.model.EventMessage;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 事件类型监听器路由和失败策略测试。
 */
class EventTypeDispatcherTest {

    @Test
    void shouldDispatchMatchingAndGlobalListenersInOrder() throws Exception {
        List<String> invokedListeners = new ArrayList<>();
        EventTypeListener global = createListener(Set.of(EventTypeListener.ALL_EVENT_TYPES),
                EventListenerFailurePolicy.RETRY_EVENT, 20, message -> invokedListeners.add("global"));
        EventTypeListener exact = createListener(Set.of("upms.user.created"),
                EventListenerFailurePolicy.RETRY_EVENT, 10, message -> invokedListeners.add("exact"));
        EventTypeListener unrelated = createListener(Set.of("order.created"),
                EventListenerFailurePolicy.RETRY_EVENT, 0, message -> invokedListeners.add("unrelated"));
        EventTypeDispatcher dispatcher = new EventTypeDispatcher(List.of(global, exact, unrelated));

        dispatcher.dispatch(createMessage());

        assertThat(invokedListeners).containsExactly("exact", "global");
    }

    @Test
    void shouldContinueWhenObserverFailureIsIgnored() throws Exception {
        List<String> invokedListeners = new ArrayList<>();
        EventTypeListener failedObserver = createListener(Set.of(EventTypeListener.ALL_EVENT_TYPES),
                EventListenerFailurePolicy.IGNORE_FAILURE, 0, message -> {
                    throw new IllegalStateException("监控写入失败");
                });
        EventTypeListener nextListener = createListener(Set.of("upms.user.created"),
                EventListenerFailurePolicy.RETRY_EVENT, 10, message -> invokedListeners.add("next"));
        EventTypeDispatcher dispatcher = new EventTypeDispatcher(List.of(nextListener, failedObserver));

        dispatcher.dispatch(createMessage());

        assertThat(invokedListeners).containsExactly("next");
    }

    @Test
    void shouldPropagateFailureWhenEventMustBeRetried() {
        EventTypeListener failedListener = createListener(Set.of("upms.user.created"),
                EventListenerFailurePolicy.RETRY_EVENT, 0, message -> {
                    throw new IllegalStateException("业务处理失败");
                });
        EventTypeDispatcher dispatcher = new EventTypeDispatcher(List.of(failedListener));

        assertThatThrownBy(() -> dispatcher.dispatch(createMessage()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("业务处理失败");
    }

    private EventTypeListener createListener(Set<String> eventTypes,
                                             EventListenerFailurePolicy failurePolicy,
                                             int order,
                                             Consumer<EventMessage> consumer) {
        return new TestEventTypeListener(eventTypes, failurePolicy, order, consumer);
    }

    private EventMessage createMessage() {
        return new EventMessage(
                "event-1", "upms.user.created", 1, Instant.now(), "upms", "user-1",
                Map.of(), new ObjectMapper().valueToTree(Map.of("userId", 1L)));
    }

    /**
     * 测试使用的可排序事件监听器。
     */
    private record TestEventTypeListener(
            Set<String> eventTypes,
            EventListenerFailurePolicy failurePolicy,
            int order,
            Consumer<EventMessage> consumer
    ) implements EventTypeListener, Ordered {

        @Override
        public void onEvent(EventMessage message) {
            consumer.accept(message);
        }

        @Override
        public int getOrder() {
            return order;
        }
    }
}
