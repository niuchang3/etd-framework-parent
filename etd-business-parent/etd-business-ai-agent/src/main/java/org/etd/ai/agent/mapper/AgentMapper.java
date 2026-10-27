package org.etd.ai.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.etd.ai.agent.entity.AgentEntity;

/** Agent单表数据访问；租户、逻辑删除及公开读取范围由现有框架处理。 */
public interface AgentMapper extends BaseMapper<AgentEntity> {
    /** @param page 分页 @param keyword 名称关键字 @param dataStatus 启停筛选 @param published 发布筛选 @return 可见卡片 */
    IPage<AgentEntity> selectReadablePage(IPage<AgentEntity> page, @Param("keyword") String keyword,
            @Param("dataStatus") Integer dataStatus, @Param("published") Boolean published);

    /** @param id Agent ID @param lock 是否在调用方事务内加行锁 @return 可访问记录及配置，不存在时为空 */
    AgentEntity selectAccessible(@Param("id") Long id, @Param("lock") boolean lock);
}
