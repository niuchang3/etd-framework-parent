package org.etd.event.eventbus.adapter;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.etd.event.message.entity.EventMessageEntity;
import org.etd.event.message.mapper.EventMessageMapper;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.starter.event.server.eventbus.model.EventMessageRegistration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 入口事件消息幂等登记测试。
 */
class EventMessageRegistrarAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EventMessageMapper messageMapper = mock(EventMessageMapper.class);
    private final EventMessageRegistrarAdapter messageRegistrar =
            new EventMessageRegistrarAdapter(messageMapper, objectMapper);

    /** 初始化 Lambda 列缓存，使单元测试环境与 MyBatis 启动后保持一致。 */
    @BeforeAll
    static void initializeTableMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(configuration, ""), EventMessageEntity.class);
    }

    @Test
    void shouldCreateMessageForFirstRegistration() {
        EventMessage message = createMessage();
        when(messageMapper.insert(any(EventMessageEntity.class))).thenAnswer(invocation -> {
            EventMessageEntity entity = invocation.getArgument(0);
            entity.setId(30L);
            return 1;
        });

        EventMessageRegistration registration =
                messageRegistrar.registerEventMessage(message, 10L);

        assertThat(registration.messageId()).isEqualTo(30L);
        assertThat(registration.created()).isTrue();
        verify(messageMapper).insert(any(EventMessageEntity.class));
    }

    @Test
    void shouldReuseMessageWhenRepeatedContentIsIdentical() {
        EventMessage message = createMessage();
        when(messageMapper.selectOne(any())).thenReturn(createPersistedMessage(message));

        EventMessageRegistration registration =
                messageRegistrar.registerEventMessage(message, 10L);

        assertThat(registration.messageId()).isEqualTo(30L);
        assertThat(registration.created()).isFalse();
        verify(messageMapper, never()).insert(any(EventMessageEntity.class));
    }

    @Test
    void shouldRejectDifferentContentUsingExistingEventId() {
        EventMessage message = createMessage();
        EventMessageEntity persisted = createPersistedMessage(message);
        persisted.setSourceApplication("order");
        when(messageMapper.selectOne(any())).thenReturn(persisted);

        assertThatThrownBy(() -> messageRegistrar.registerEventMessage(message, 10L))
                .isInstanceOf(ApiRuntimeException.class)
                .hasMessageContaining("事件 ID 已被不同的消息内容使用");
    }

    private EventMessage createMessage() {
        return new EventMessage(
                "event-1", "upms.user.created", 1,
                Instant.parse("2026-09-15T01:00:00Z"), "upms", "user-1",
                Map.of("traceId", "trace-1"),
                objectMapper.valueToTree(Map.of("userId", 1L)));
    }

    private EventMessageEntity createPersistedMessage(EventMessage message) {
        EventMessageEntity entity = new EventMessageEntity();
        entity.setId(30L);
        entity.setEventId(message.eventId());
        entity.setEventTypeId(10L);
        entity.setEventVersion(message.eventVersion());
        entity.setOccurredAt(message.occurredAt());
        entity.setSourceApplication(message.source());
        entity.setPartitionKey(message.partitionKey());
        entity.setEventContext(objectMapper.valueToTree(message.context()));
        entity.setEventPayload(message.payload());
        return entity;
    }
}
