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

    /**
     * 校验入口事件与已启用的事件类型定义一致。
     *
     * @param eventType 事件类型编码
     * @param sourceApplication 事件来源应用
     * @param eventVersion 消息协议版本
     * @return 匹配的事件类型实体
     */
    EventTypeEntity requireEventTypeByMessage(String eventType, String sourceApplication, int eventVersion);

    Long create(EventTypeSaveDTO dto);
    boolean modify(Long id, EventTypeUpdateDTO dto);
    boolean switchEnabled(Long id, Boolean enabled);
    boolean remove(Long id);
}
