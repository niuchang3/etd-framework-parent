package org.etd.ai.knowledge.mapper;

import org.apache.ibatis.annotations.Param;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;

/** 固定维度向量副本写入；调用方负责权限、版本和事务。 */
public interface KnowledgeVectorMapper {
    /** @param table 后端维度白名单表名 @param id 雪花记录ID @param document 已授权资料
     * @param chunkId 分片ID @param content 模型输入正文 @param connectionId 向量连接
     * @param indexVersion 索引代次 @param vector 已校验的向量字面量 @return 插入行数 */
    int insertVector(@Param("table") String table, @Param("id") Long id,
            @Param("document") KnowledgeDocumentEntity document, @Param("chunkId") Long chunkId,
            @Param("content") String content, @Param("connectionId") Long connectionId,
            @Param("indexVersion") Long indexVersion, @Param("vector") String vector);

    /** @param table 后端白名单表名 @param documentId 已授权资料 @return 清理行数 */
    int deleteByDocument(@Param("table") String table, @Param("documentId") Long documentId);
}
