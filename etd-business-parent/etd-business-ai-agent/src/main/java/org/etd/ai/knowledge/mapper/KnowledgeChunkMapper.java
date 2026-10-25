package org.etd.ai.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.etd.ai.knowledge.entity.KnowledgeChunkEntity;
import org.apache.ibatis.annotations.Param;

/** 分片表访问；清理操作始终由已校验资料权限的编排调用。 */
public interface KnowledgeChunkMapper extends BaseMapper<KnowledgeChunkEntity> {
    /** @param documentId 已校验的资料ID @return 物理清理的分片数，释放原有顺序唯一约束 */
    int deleteByDocument(@Param("documentId") Long documentId);
}
