package org.etd.ai.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Param;
import org.etd.ai.model.entity.ModelConfigEntity;

/** ModelConfigEntity 数据访问；租户隔离由框架及业务查询共同限定。 */
public interface ModelConfigMapper extends BaseMapper<ModelConfigEntity> {
    /**
     * 检查有效知识库引用，索引由知识库模型复合索引覆盖。
     *
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @return 符合当前租户及查询条件的记录数量
     */
    Long selectKnowledgeBaseCount(@Param("id") Long id);
}
