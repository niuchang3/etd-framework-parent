package org.etd.ai.knowledge.service;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.entity.KnowledgeBaseEntity;
import org.etd.ai.knowledge.mapper.KnowledgeRetrievalMapper;
import org.etd.ai.knowledge.model.KnowledgeSearchHit;
import org.etd.ai.knowledge.constant.KnowledgeRetrievalPolicy;
import org.etd.framework.starter.mybaits.permission.model.DataPermissionScope;
import org.springframework.stereotype.Service;
import java.util.List;
/** 检索候选读取能力；范围、公开与发布状态在数据库截断前统一过滤。 */
@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalService {
    private final KnowledgeRetrievalMapper mapper;
    /** @param documentId 已校验维护权限并锁定的文档，清理所有维度及版本。 */
    public void removeDocumentVectors(Long documentId) {
        for (Integer dimensions : KnowledgeRetrievalPolicy.DIMENSIONS) {
            String table = KnowledgeRetrievalPolicy.vectorTable(dimensions);
            mapper.deleteDocumentVectors(table, documentId);
        }
    }
    /** @param base 生效库 @param documentId 已授权文档 @return 当前内容是否全部具备向量 */
    public boolean isDocumentIndexed(KnowledgeBaseEntity base, Long documentId) {
        if (base.getActiveDimensions() == null || base.getActiveIndexVersion() == 0) {
            return false;
        }
        String table = KnowledgeRetrievalPolicy.vectorTable(base.getActiveDimensions());
        return mapper.selectDocumentMissingCount(base, table, documentId) == 0;
    }

    /** @param base 库 @param query 问题 @param status 可选状态 @return 全文候选 */
    public List<KnowledgeSearchHit> fetchKeywordList(KnowledgeBaseEntity base, String query, Integer status) {
        var scope = DataPermissionScope.current();
        return mapper.selectKeywordList(base, query, scope, status);
    }
    /** @param base 生效索引配置 @param vector 查询向量文本 @param status 状态 @return 向量候选 */
    public List<KnowledgeSearchHit> fetchVectorList(KnowledgeBaseEntity base, String vector, Integer status) {
        var scope = DataPermissionScope.current();
        String table = KnowledgeRetrievalPolicy.vectorTable(base.getActiveDimensions());
        return mapper.selectVectorList(base, table, vector, scope, status);
    }
}
