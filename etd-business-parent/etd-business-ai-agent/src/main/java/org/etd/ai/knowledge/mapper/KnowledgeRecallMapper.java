package org.etd.ai.knowledge.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.etd.ai.knowledge.model.KnowledgeRecallHit;

/** 召回场景查询，实时关联知识库权限、文档公开状态、确认状态和向量内容轮次。 */
public interface KnowledgeRecallMapper {
    /** @param baseId 已授权知识库 @param query 问题 @param size 候选上限 @param test 是否允许禁用资料
     * @return 按全文分数排列的候选 */
    List<KnowledgeRecallHit> selectKeywordList(@Param("baseId") Long baseId, @Param("query") String query,
            @Param("size") int size, @Param("test") boolean test);
    /** @param baseId 已授权知识库 @param table 固定维度白名单表名 @param vector 查询向量
     * @param connectionId 生效模型连接 @param indexVersion 生效代次 @param threshold 可选余弦下限
     * @param size 候选上限 @param test 是否允许禁用资料 @return 按余弦距离排列的候选 */
    List<KnowledgeRecallHit> selectVectorList(@Param("baseId") Long baseId, @Param("table") String table,
            @Param("vector") String vector, @Param("connectionId") Long connectionId,
            @Param("indexVersion") Long indexVersion, @Param("threshold") Double threshold,
            @Param("size") int size, @Param("test") boolean test);
    /** @param baseId 已授权知识库 @param ids 候选分片ID @param test 是否允许禁用资料
     * @return 外部模型返回后仍有读取权限且已确认的当前正文 */
    List<KnowledgeRecallHit> selectCurrentList(@Param("baseId") Long baseId, @Param("ids") List<Long> ids,
            @Param("test") boolean test);
}
