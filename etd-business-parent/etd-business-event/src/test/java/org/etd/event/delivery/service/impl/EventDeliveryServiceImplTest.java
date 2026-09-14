package org.etd.event.delivery.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.etd.event.delivery.constant.EventDeliveryStatus;
import org.etd.event.delivery.entity.EventDeliveryEntity;
import org.etd.event.delivery.mapper.EventDeliveryMapper;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 人工重播状态边界测试。
 */
class EventDeliveryServiceImplTest {

    private final EventDeliveryMapper deliveryMapper = mock(EventDeliveryMapper.class);
    private final EventDeliveryServiceImpl deliveryService = new EventDeliveryServiceImpl(deliveryMapper);

    /** 初始化 Lambda 列缓存，使单元测试环境与 MyBatis 启动后的行为一致。 */
    @BeforeAll
    static void initializeTableMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), EventDeliveryEntity.class);
    }

    @Test
    void shouldResetDeadDeliveryForTargetedReplay() {
        EventDeliveryEntity entity = createDelivery(EventDeliveryStatus.DEAD);
        when(deliveryMapper.selectOne(any())).thenReturn(entity);
        when(deliveryMapper.update(isNull(), any())).thenReturn(1);

        assertTrue(deliveryService.replayFailedDelivery("event-100", 100L));

        verify(deliveryMapper).update(isNull(), any());
    }

    @Test
    void shouldRejectSucceededDeliveryReplay() {
        EventDeliveryEntity entity = createDelivery(EventDeliveryStatus.SUCCEEDED);
        when(deliveryMapper.selectOne(any())).thenReturn(entity);

        assertThrows(ApiRuntimeException.class,
                () -> deliveryService.replayFailedDelivery("event-100", 100L));
    }

    private EventDeliveryEntity createDelivery(EventDeliveryStatus status) {
        EventDeliveryEntity entity = new EventDeliveryEntity();
        entity.setId(100L);
        entity.setEventId("event-100");
        entity.setVersion(2);
        entity.setDeliveryStatus(status.getCode());
        return entity;
    }
}
