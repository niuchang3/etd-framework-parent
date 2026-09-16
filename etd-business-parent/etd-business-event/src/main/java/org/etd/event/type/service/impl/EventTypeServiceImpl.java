package org.etd.event.type.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.etd.event.type.controller.dto.EventTypeSaveDTO;
import org.etd.event.type.controller.dto.EventTypeUpdateDTO;
import org.etd.event.type.controller.vo.EventTypeVO;
import org.etd.event.type.entity.EventTypeEntity;
import org.etd.event.type.mapper.EventTypeMapper;
import org.etd.event.type.service.EventTypeService;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 事件类型配置基础能力实现。
 */
@Service
public class EventTypeServiceImpl implements EventTypeService {

    private final EventTypeMapper eventTypeMapper;

    public EventTypeServiceImpl(EventTypeMapper eventTypeMapper) {
        this.eventTypeMapper = eventTypeMapper;
    }

    @Override
    public IPage<EventTypeVO> page(long current, long size, String keyword,
                                   String sourceApplication, Boolean enabled) {
        LambdaQueryWrapper<EventTypeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(sourceApplication), EventTypeEntity::getSourceApplication, sourceApplication);
        if (enabled != null) {
            wrapper.eq(EventTypeEntity::getDataStatus, statusCode(enabled));
        }
        wrapper.and(StringUtils.hasText(keyword), query -> query
                        .like(EventTypeEntity::getEventType, keyword)
                        .or().like(EventTypeEntity::getEventName, keyword))
                .orderByDesc(EventTypeEntity::getUpdateTime)
                .orderByDesc(EventTypeEntity::getId);
        return eventTypeMapper.selectPage(new Page<>(current, size), wrapper).convert(this::toVO);
    }

    @Override
    public List<EventTypeVO> selectEnabledList() {
        LambdaQueryWrapper<EventTypeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventTypeEntity::getDataStatus, BasicConstant.DataStatus.ENABLED_CODE)
                .orderByAsc(EventTypeEntity::getEventType);
        return eventTypeMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public EventTypeVO fetchById(Long id) {
        return toVO(requireEntity(id));
    }

    @Override
    public EventTypeEntity requireEntity(Long id) {
        EventTypeEntity entity = eventTypeMapper.selectById(id);
        if (entity == null) {
            throw new ApiRuntimeException("事件类型不存在。");
        }
        return entity;
    }

    @Override
    public Long create(EventTypeSaveDTO dto) {
        ensureTypeAvailable(dto.getEventType(), null);
        EventTypeEntity entity = toEntity(dto);
        eventTypeMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public boolean modify(Long id, EventTypeUpdateDTO dto) {
        EventTypeEntity current = requireEntity(id);
        if (!current.getEventType().equals(dto.getContent().getEventType())) {
            throw new ApiRuntimeException("事件类型编码创建后不允许修改。");
        }
        ensureTypeAvailable(dto.getContent().getEventType(), id);
        EventTypeEntity entity = toEntity(dto.getContent());
        entity.setId(id);
        entity.setVersion(dto.getVersion());
        ensureUpdated(eventTypeMapper.updateById(entity), "事件类型已被其他用户修改，请刷新后重试。");
        return true;
    }

    @Override
    public boolean switchEnabled(Long id, Boolean enabled) {
        EventTypeEntity current = requireEntity(id);
        EventTypeEntity entity = new EventTypeEntity();
        entity.setId(id);
        entity.setVersion(current.getVersion());
        entity.setDataStatus(statusCode(enabled));
        ensureUpdated(eventTypeMapper.updateById(entity), "事件类型状态已变化，请刷新后重试。");
        return true;
    }

    @Override
    public boolean remove(Long id) {
        requireEntity(id);
        return eventTypeMapper.deleteById(id) > 0;
    }

    private void ensureTypeAvailable(String eventType, Long excludedId) {
        LambdaQueryWrapper<EventTypeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventTypeEntity::getEventType, eventType)
                .ne(excludedId != null, EventTypeEntity::getId, excludedId);
        if (eventTypeMapper.selectCount(wrapper) > 0) {
            throw new ApiRuntimeException("事件类型编码已存在。");
        }
    }

    private EventTypeEntity toEntity(EventTypeSaveDTO dto) {
        EventTypeEntity entity = new EventTypeEntity();
        entity.setEventType(dto.getEventType());
        entity.setEventName(dto.getEventName());
        entity.setSourceApplication(dto.getSourceApplication());
        entity.setLatestVersion(dto.getLatestVersion());
        entity.setDescription(dto.getDescription());
        return entity;
    }

    private EventTypeVO toVO(EventTypeEntity entity) {
        EventTypeVO vo = new EventTypeVO();
        vo.setId(entity.getId());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        vo.setVersion(entity.getVersion());
        vo.setEventType(entity.getEventType());
        vo.setEventName(entity.getEventName());
        vo.setSourceApplication(entity.getSourceApplication());
        vo.setLatestVersion(entity.getLatestVersion());
        vo.setEnabled(BasicConstant.DataStatus.ENABLED_CODE == entity.getDataStatus());
        vo.setDescription(entity.getDescription());
        return vo;
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
