package org.etd.event.eventbus.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.etd.event.subscription.entity.EventSubscriptionEntity;
import org.etd.event.subscription.mapper.EventSubscriptionMapper;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.starter.event.server.eventbus.model.EventSubscriptionTarget;
import org.etd.framework.starter.event.server.eventbus.port.EventSubscriptionResolver;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于订阅配置表的事件总线订阅目标适配器。
 */
@Component
public class EventSubscriptionResolverAdapter implements EventSubscriptionResolver {

    private final EventSubscriptionMapper subscriptionMapper;

    public EventSubscriptionResolverAdapter(EventSubscriptionMapper subscriptionMapper) {
        this.subscriptionMapper = subscriptionMapper;
    }

    /**
     * 将当前启用订阅转换为稳定的投递目标快照。
     *
     * @param eventTypeId 事件类型主键
     * @return 订阅目标快照列表
     */
    @Override
    public List<EventSubscriptionTarget> resolveSubscriptionTargetList(Long eventTypeId) {
        LambdaQueryWrapper<EventSubscriptionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventSubscriptionEntity::getEventTypeId, eventTypeId)
                .eq(EventSubscriptionEntity::getDataStatus, BasicConstant.DataStatus.ENABLED_CODE)
                .orderByAsc(EventSubscriptionEntity::getId);
        List<EventSubscriptionEntity> subscriptionList = subscriptionMapper.selectList(wrapper);
        List<EventSubscriptionTarget> targetList = new ArrayList<>(subscriptionList.size());
        for (EventSubscriptionEntity subscription : subscriptionList) {
            targetList.add(new EventSubscriptionTarget(
                    subscription.getId(), subscription.getTargetTopic()));
        }
        return List.copyOf(targetList);
    }
}
