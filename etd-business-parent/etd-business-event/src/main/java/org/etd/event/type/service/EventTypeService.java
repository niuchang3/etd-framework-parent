package org.etd.event.type.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.etd.event.type.controller.dto.EventTypeSaveDTO;
import org.etd.event.type.controller.dto.EventTypeUpdateDTO;
import org.etd.event.type.controller.vo.EventTypeVO;
import org.etd.event.type.entity.EventTypeEntity;

import java.util.List;

/**
 * 事件类型配置基础能力。
 */
public interface EventTypeService {
    IPage<EventTypeVO> page(long current, long size, String keyword, String sourceApplication, Boolean enabled);
    List<EventTypeVO> selectEnabledList();
    EventTypeVO fetchById(Long id);
    EventTypeEntity requireEntity(Long id);

    Long create(EventTypeSaveDTO dto);
    boolean modify(Long id, EventTypeUpdateDTO dto);
    boolean switchEnabled(Long id, Boolean enabled);
    boolean remove(Long id);
}
