package org.etd.ai.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.etd.ai.knowledge.entity.KnowledgeChunkEntity;

/** 正文分片及对应向量清理的数据访问。 */
public interface KnowledgeChunkMapper extends BaseMapper<KnowledgeChunkEntity> {
    /** @param documentId 已校验维护权限且锁定的资料ID */
    int deleteDocumentChunks(@Param("documentId") Long documentId);
    /** @param documentId 已校验维护权限且锁定的资料ID @param dimension 后端固定维度 */
    int deleteDocumentVectors(@Param("documentId") Long documentId, @Param("dimension") int dimension);
}
