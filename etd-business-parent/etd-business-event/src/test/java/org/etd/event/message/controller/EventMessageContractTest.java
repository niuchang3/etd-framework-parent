package org.etd.event.message.controller;

import org.etd.event.message.controller.vo.EventMessageVO;
import org.etd.event.message.entity.EventMessageEntity;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 事件消息接口模型与初始化数据库结构契约测试。
 */
class EventMessageContractTest {

    /**
     * 消息查询接口必须公开原始类型、处理状态和失败原因。
     */
    @Test
    void shouldExposeMessageResultFields() throws Exception {
        assertNotNull(EventMessageVO.class.getDeclaredField("eventType"));
        assertNotNull(EventMessageVO.class.getDeclaredField("messageStatus"));
        assertNotNull(EventMessageVO.class.getDeclaredField("failureReason"));
        assertNotNull(EventMessageEntity.class.getDeclaredField("eventType"));
        assertNotNull(EventMessageEntity.class.getDeclaredField("messageStatus"));
        assertNotNull(EventMessageEntity.class.getDeclaredField("failureReason"));
    }

    /**
     * 初始化 SQL 必须允许类型解析失败，并约束消息状态与失败原因保持一致。
     */
    @Test
    void shouldDeclareMessageFailureColumnsInSchema() throws Exception {
        try (var inputStream = getClass().getClassLoader()
                .getResourceAsStream("sql/postgresql/schema.sql")) {
            assertNotNull(inputStream);
            String schema = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(schema.contains("event_type         varchar(150) not null"));
            assertTrue(schema.contains("event_type_id      bigint references evt_event_type (id)"));
            assertTrue(schema.contains("message_status     integer not null default 1"));
            assertTrue(schema.contains("failure_reason     varchar(2000)"));
            assertTrue(schema.contains("idx_evt_event_message_%1$s_status_time"));
        }
    }
}
