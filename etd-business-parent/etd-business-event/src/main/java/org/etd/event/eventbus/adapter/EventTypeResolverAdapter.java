package org.etd.event.eventbus.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.etd.event.type.entity.EventTypeEntity;
import org.etd.event.type.mapper.EventTypeMapper;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.starter.event.server.eventbus.model.EventTypeDefinition;
import org.etd.framework.starter.event.server.eventbus.port.EventTypeResolver;
import org.springframework.stereotype.Component;

/**
 * 基于事件类型配置表的事件类型事实查询适配器。
 */
@Component
public class EventTypeResolverAdapter implements EventTypeResolver {

    private final EventTypeMapper eventTypeMapper;

    public EventTypeResolverAdapter(EventTypeMapper eventTypeMapper) {
        this.eventTypeMapper = eventTypeMapper;
    }

    /**
     * 查询事件类型事实并转换为 starter-server 统一模型。
     *
     * @param eventType 事件类型编码
     * @return 事件类型事实快照，不存在时返回 {@code null}
     */
    @Override
    public EventTypeDefinition selectEventType(String eventType) {
        LambdaQueryWrapper<EventTypeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventTypeEntity::getEventType, eventType);
        EventTypeEntity entity = eventTypeMapper.selectOne(wrapper);
        if (entity == null) {
            return null;
        }
        boolean enabled = entity.getDataStatus() == BasicConstant.DataStatus.ENABLED_CODE;
        return new EventTypeDefinition(
                entity.getId(), entity.getEventType(), entity.getSourceApplication(),
                entity.getLatestVersion(), enabled);
    }
}
