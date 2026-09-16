package org.etd.event.eventbus.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.etd.event.type.entity.EventTypeEntity;
import org.etd.event.type.mapper.EventTypeMapper;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.event.core.model.EventMessage;
import org.etd.framework.starter.event.server.eventbus.port.EventTypeResolver;
import org.springframework.stereotype.Component;

/**
 * 基于事件类型配置表的事件总线类型解析适配器。
 */
@Component
public class EventTypeResolverAdapter implements EventTypeResolver {

    private final EventTypeMapper eventTypeMapper;

    public EventTypeResolverAdapter(EventTypeMapper eventTypeMapper) {
        this.eventTypeMapper = eventTypeMapper;
    }

    /**
     * 校验事件类型、来源应用和协议版本，并返回事件类型主键。
     *
     * @param message 统一事件消息
     * @return 事件类型主键
     */
    @Override
    public Long resolveEventTypeId(EventMessage message) {
        LambdaQueryWrapper<EventTypeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventTypeEntity::getEventType, message.eventType());
        EventTypeEntity entity = eventTypeMapper.selectOne(wrapper);
        ensureEnabled(entity, message.eventType());
        ensureMessageMatchesType(entity, message);
        return entity.getId();
    }

    private void ensureEnabled(EventTypeEntity entity, String eventType) {
        if (entity == null || entity.getDataStatus() != BasicConstant.DataStatus.ENABLED_CODE) {
            throw new ApiRuntimeException("事件类型不存在或未启用：" + eventType);
        }
    }

    private void ensureMessageMatchesType(EventTypeEntity entity, EventMessage message) {
        if (!entity.getSourceApplication().equals(message.source())) {
            throw new ApiRuntimeException("事件来源应用与事件类型定义不一致。");
        }
        if (message.eventVersion() > entity.getLatestVersion()) {
            throw new ApiRuntimeException("事件协议版本高于事件中心登记的最新版本。");
        }
    }
}
