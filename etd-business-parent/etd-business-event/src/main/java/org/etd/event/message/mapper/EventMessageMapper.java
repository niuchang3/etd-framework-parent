package org.etd.event.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.etd.event.message.entity.EventMessageEntity;

/**
 * 原始事件消息数据访问接口，逻辑表由 ShardingSphere 路由到固定物理分表。
 */
@Mapper
public interface EventMessageMapper extends BaseMapper<EventMessageEntity> {
}
