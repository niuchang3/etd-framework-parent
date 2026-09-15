package org.etd.event.subscription.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.etd.event.subscription.controller.dto.EventSubscriptionSaveDTO;
import org.etd.event.subscription.controller.dto.EventSubscriptionUpdateDTO;
import org.etd.event.subscription.controller.vo.EventSubscriptionVO;
import org.etd.event.subscription.entity.EventSubscriptionEntity;
import org.etd.event.subscription.mapper.EventSubscriptionMapper;
import org.etd.event.subscription.service.EventSubscriptionService;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 事件订阅配置基础能力实现。
 */
@Service
public class EventSubscriptionServiceImpl implements EventSubscriptionService {

    private final EventSubscriptionMapper subscriptionMapper;

    public EventSubscriptionServiceImpl(EventSubscriptionMapper subscriptionMapper) {
        this.subscriptionMapper = subscriptionMapper;
    }

    @Override
    public IPage<EventSubscriptionVO> page(long current, long size, String keyword, Long eventTypeId,
                                           String subscriberApplication, Boolean enabled) {
        Integer dataStatus = enabled == null ? null : statusCode(enabled);
        return subscriptionMapper.selectSubscriptionPage(new Page<>(current, size), keyword,
                eventTypeId, subscriberApplication, dataStatus);
    }

    @Override
    public List<EventSubscriptionVO> selectEnabledList() {
        return subscriptionMapper.selectEnabledList();
    }

    @Override
    public List<EventSubscriptionEntity> selectEnabledSubscriptionListByEventTypeId(Long eventTypeId) {
        LambdaQueryWrapper<EventSubscriptionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventSubscriptionEntity::getEventTypeId, eventTypeId)
                .eq(EventSubscriptionEntity::getDataStatus, BasicConstant.DataStatus.ENABLED_CODE)
                .orderByAsc(EventSubscriptionEntity::getId);
        return subscriptionMapper.selectList(wrapper);
    }

    @Override
    public EventSubscriptionVO fetchById(Long id) {
        EventSubscriptionVO vo = subscriptionMapper.selectSubscriptionById(id);
        if (vo == null) {
            throw new ApiRuntimeException("事件订阅不存在。");
        }
        return vo;
    }

    @Override
    public EventSubscriptionEntity requireEntity(Long id) {
        EventSubscriptionEntity entity = subscriptionMapper.selectById(id);
        if (entity == null) {
            throw new ApiRuntimeException("事件订阅不存在。");
        }
        return entity;
    }

    @Override
    public boolean existsByEventTypeId(Long eventTypeId) {
        return subscriptionMapper.countByEventTypeId(eventTypeId) > 0;
    }

    @Override
    public Long create(EventSubscriptionSaveDTO dto) {
        ensureApplicationTypeAvailable(dto, null);
        EventSubscriptionEntity entity = toEntity(dto);
        subscriptionMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public boolean modify(Long id, EventSubscriptionUpdateDTO dto) {
        requireEntity(id);
        ensureApplicationTypeAvailable(dto.getContent(), id);
        EventSubscriptionEntity entity = toEntity(dto.getContent());
        entity.setId(id);
        entity.setVersion(dto.getVersion());
        ensureUpdated(subscriptionMapper.updateById(entity), "订阅已被其他用户修改，请刷新后重试。");
        return true;
    }

    @Override
    public boolean switchEnabled(Long id, Boolean enabled) {
        EventSubscriptionEntity current = requireEntity(id);
        EventSubscriptionEntity entity = new EventSubscriptionEntity();
        entity.setId(id);
        entity.setVersion(current.getVersion());
        entity.setDataStatus(statusCode(enabled));
        ensureUpdated(subscriptionMapper.updateById(entity), "订阅状态已变化，请刷新后重试。");
        return true;
    }

    @Override
    public boolean remove(Long id) {
        requireEntity(id);
        return subscriptionMapper.deleteById(id) > 0;
    }

    private void ensureApplicationTypeAvailable(EventSubscriptionSaveDTO dto, Long excludedId) {
        LambdaQueryWrapper<EventSubscriptionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventSubscriptionEntity::getSubscriberApplication, dto.getSubscriberApplication())
                .eq(EventSubscriptionEntity::getEventTypeId, dto.getEventTypeId())
                .ne(excludedId != null, EventSubscriptionEntity::getId, excludedId);
        if (subscriptionMapper.selectCount(wrapper) > 0) {
            throw new ApiRuntimeException("该应用已经订阅当前事件类型。");
        }
    }

    private EventSubscriptionEntity toEntity(EventSubscriptionSaveDTO dto) {
        EventSubscriptionEntity entity = new EventSubscriptionEntity();
        entity.setSubscriptionName(dto.getSubscriptionName());
        entity.setEventTypeId(dto.getEventTypeId());
        entity.setSubscriberApplication(dto.getSubscriberApplication());
        entity.setTargetTopic(dto.getTargetTopic());
        entity.setDescription(dto.getDescription());
        return entity;
    }

    private int statusCode(boolean enabled) {
        return enabled ? BasicConstant.DataStatus.ENABLED_CODE : BasicConstant.DataStatus.DISABLED_CODE;
    }

    private void ensureUpdated(int count, String message) {
        if (count == 0) {
            throw new ApiRuntimeException(message);
        }
    }
}
