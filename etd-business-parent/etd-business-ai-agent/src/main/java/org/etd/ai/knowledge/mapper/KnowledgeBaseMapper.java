package org.etd.ai.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.etd.ai.knowledge.entity.KnowledgeBaseEntity;

/** KnowledgeBaseEntity 数据访问；租户隔离由框架及业务查询共同限定。 */
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBaseEntity> {}
