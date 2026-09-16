package org.etd.event.delivery.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.etd.event.delivery.constant.EventDeliveryStatus;
import org.etd.event.delivery.entity.EventDeliveryEntity;
import org.etd.event.delivery.mapper.EventDeliveryMapper;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 人工重播状态边界测试。
 */
class EventDeliveryReplayServiceImplTest {

    private final EventDeliveryMapper deliveryMapper = mock(EventDeliveryMapper.class);
    private final EventDeliveryReplayServiceImpl replayService =
            new EventDeliveryReplayServiceImpl(deliveryMapper);

    /** 初始化 Lambda 列缓存，使单元测试环境与 MyBatis 启动后的行为一致。 */
    @BeforeAll
    static void initializeTableMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(configuration, ""), EventDeliveryEntity.class);
    }

    @Test
    void shouldResetDeadDeliveryForTargetedReplay() {
        when(deliveryMapper.selectOne(any())).thenReturn(createDelivery(EventDeliveryStatus.DEAD));
        when(deliveryMapper.update(isNull(), any())).thenReturn(1);

        assertDoesNotThrow(() -> replayService.resetDeliveryForReplay("event-100", 100L));

        verify(deliveryMapper).update(isNull(), any());
    }

    @Test
    void shouldRejectSucceededDeliveryReplay() {
        when(deliveryMapper.selectOne(any())).thenReturn(
                createDelivery(EventDeliveryStatus.SUCCEEDED));

        assertThrows(ApiRuntimeException.class,
                () -> replayService.resetDeliveryForReplay("event-100", 100L));
    }

    private EventDeliveryEntity createDelivery(EventDeliveryStatus status) {
        EventDeliveryEntity delivery = new EventDeliveryEntity();
        delivery.setId(100L);
        delivery.setEventId("event-100");
        delivery.setVersion(2);
        delivery.setDeliveryStatus(status.getCode());
        return delivery;
    }
}
