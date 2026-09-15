package org.etd.event.subscription.controller;

import org.etd.event.subscription.controller.dto.EventSubscriptionSaveDTO;
import org.etd.event.subscription.controller.vo.EventSubscriptionVO;
import org.etd.event.subscription.entity.EventSubscriptionEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 事件订阅接口与数据库结构契约测试。
 */
class EventSubscriptionContractTest {

    /** 已删除的订阅编码和消费组不得重新出现在接口或持久化模型中。 */
    private static final Set<String> REMOVED_FIELD_NAMES = Set.of("subscriptionCode", "consumerGroup");

    /**
     * 订阅请求、响应和实体统一使用雪花主键，不再暴露重复业务编码或消费端配置。
     */
    @Test
    void shouldExcludeRemovedFieldsFromSubscriptionModels() {
        assertRemovedFieldsAbsent(EventSubscriptionSaveDTO.class);
        assertRemovedFieldsAbsent(EventSubscriptionVO.class);
        assertRemovedFieldsAbsent(EventSubscriptionEntity.class);
    }

    /**
     * 初始化 SQL 必须直接表达当前最终表结构，不保留已废弃列。
     */
    @Test
    void shouldExcludeRemovedColumnsFromSchema() throws Exception {
        try (var inputStream = getClass().getClassLoader().getResourceAsStream("sql/postgresql/schema.sql")) {
            assertNotNull(inputStream);
            String schema = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertFalse(schema.contains("subscription_code"));
            assertFalse(schema.contains("consumer_group"));
        }
    }

    private void assertRemovedFieldsAbsent(Class<?> modelClass) {
        Set<String> declaredFieldNames = Arrays.stream(modelClass.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
        assertFalse(declaredFieldNames.removeAll(REMOVED_FIELD_NAMES));
    }
}
