package org.etd.ai.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Param;
import org.etd.ai.model.entity.ModelConnectionEntity;

/** ModelConnectionEntity 数据访问；租户隔离由框架及业务查询共同限定。 */
public interface ModelConnectionMapper extends BaseMapper<ModelConnectionEntity> {
    /**
     * 统计使用该连接的有效知识库，禁止更改已有向量空间的端点。
     *
     * @param id 连接标识
     * @return 引用该连接的有效知识库数量
     */
    Long selectBoundKnowledgeBaseCount(@Param("id") Long id);
}
