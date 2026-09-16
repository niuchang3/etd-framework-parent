package org.etd.event.delivery.adapter;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.etd.event.delivery.constant.EventDeliveryStatus;
import org.etd.event.delivery.converter.EventDeliveryTaskConverter;
import org.etd.event.delivery.entity.EventDeliveryEntity;
import org.etd.event.delivery.mapper.EventDeliveryMapper;
import org.etd.event.eventbus.adapter.EventDeliveryTaskAdapter;
import org.etd.framework.starter.event.server.delivery.model.EventDeliveryState;
import org.etd.framework.starter.event.server.delivery.model.EventDeliveryStatusEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 可靠投递任务状态幂等更新测试。
 */
class EventDeliveryTaskStatusUpdateTest {

    private final EventDeliveryMapper deliveryMapper = mock(EventDeliveryMapper.class);
    private final EventDeliveryTaskAdapter taskRegistrar =
            new EventDeliveryTaskAdapter(deliveryMapper, new EventDeliveryTaskConverter());

    /** 初始化 Lambda 列缓存，使单元测试环境与 MyBatis 启动后的行为一致。 */
    @BeforeAll
    static void initializeTableMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(configuration, ""), EventDeliveryEntity.class);
    }

    @Test
    void shouldProjectSucceededStatus() {
        when(deliveryMapper.selectOne(any())).thenReturn(
                createDelivery(EventDeliveryStatus.PUBLISHING));
        when(deliveryMapper.update(isNull(), any())).thenReturn(1);

        taskRegistrar.updateDeliveryStatus(createSucceededStatus());

        verify(deliveryMapper).update(isNull(), any());
    }

    @Test
    void shouldIgnoreDuplicateSucceededStatus() {
        when(deliveryMapper.selectOne(any())).thenReturn(
                createDelivery(EventDeliveryStatus.SUCCEEDED));

        taskRegistrar.updateDeliveryStatus(createSucceededStatus());

        verify(deliveryMapper, never()).update(isNull(), any());
    }

    private EventDeliveryStatusEvent createSucceededStatus() {
        return new EventDeliveryStatusEvent(
                100L, "event-100", EventDeliveryState.SUCCEEDED, 2,
                "event-sub-order-topic", 3, 18L, null,
                Instant.parse("2026-09-16T02:00:00Z"), null);
    }

    private EventDeliveryEntity createDelivery(EventDeliveryStatus status) {
        EventDeliveryEntity delivery = new EventDeliveryEntity();
        delivery.setId(100L);
        delivery.setEventId("event-100");
        delivery.setVersion(2);
        delivery.setDeliveryStatus(status.getCode());
        delivery.setAttemptCount(2);
        delivery.setTargetTopic("event-sub-order-topic");
        return delivery;
    }
}
