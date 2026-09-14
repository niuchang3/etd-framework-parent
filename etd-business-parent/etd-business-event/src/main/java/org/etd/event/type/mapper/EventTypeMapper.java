package org.etd.event.type.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.etd.event.type.entity.EventTypeEntity;

/**
 * 事件类型定义数据访问接口。
 */
@Mapper
public interface EventTypeMapper extends BaseMapper<EventTypeEntity> {
}
