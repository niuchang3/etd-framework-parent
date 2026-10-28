package org.etd.ai.knowledge.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.constant.KnowledgeRetrievalPolicy;
import org.etd.ai.knowledge.entity.KnowledgeBaseEntity;
import org.etd.ai.knowledge.entity.KnowledgeChunkEntity;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;
import org.etd.ai.knowledge.mapper.KnowledgeVectorMapper;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermission;

/** pgvector副本持久化；不调用外部模型，替换与分片确认处于同一短事务。 */
// 向量副本没有org_id；维护资格由Biz持有的资料行锁保证，租户拦截仍然生效。
@DataPermission(enable = false)
@Service
@RequiredArgsConstructor
public class KnowledgeVectorService {
    private final KnowledgeVectorMapper mapper;

    /** @param document 已锁定资料 @param base 已锁定知识库 @param chunks 当前正文
     * @param vectors 与正文逐条对应的向量，空表示仅启用全文索引 */
    public void replaceVectorList(KnowledgeDocumentEntity document, KnowledgeBaseEntity base,
            List<KnowledgeChunkEntity> chunks, List<float[]> vectors) {
        removeVectorList(document.getId());
        if (vectors.isEmpty()) {
            return;
        }
        if (vectors.size() != chunks.size()) {
            throw new ApiRuntimeException("向量数量与分片不一致");
        }
        String table = resolveTable(base.getActiveDimensions());
        for (int index = 0; index < chunks.size(); index++) {
            KnowledgeChunkEntity chunk = chunks.get(index);
            Long id = IdWorker.getId();
            String vector = formatVector(vectors.get(index));
            mapper.insertVector(table, id, document, chunk.getId(), chunk.getContent(),
                    base.getActiveConnectionId(), base.getActiveIndexVersion(), vector);
        }
    }

    /** @param documentId 已授权资料，删除和重新确认时均清理全部维度副本 */
    public void removeVectorList(Long documentId) {
        for (int dimensions : KnowledgeRetrievalPolicy.DIMENSIONS) {
            String table = resolveTable(dimensions);
            mapper.deleteByDocument(table, documentId);
        }
    }

    /** @param dimensions 系统允许的维度 @return 固定表名，禁止将客户端字符串拼入SQL */
    public static String resolveTable(Integer dimensions) {
        if (dimensions == null) {
            throw new ApiRuntimeException("请配置向量维度");
        }
        return switch (dimensions) {
            case 768 -> "ai_vector_768";
            case 1024 -> "ai_vector_1024";
            case 1536 -> "ai_vector_1536";
            default -> throw new ApiRuntimeException("不支持的向量维度");
        };
    }

    /** @param vector 已校验的有限向量 @return 使用绑定参数传给PostgreSQL的向量字面量 */
    public static String formatVector(float[] vector) {
        return Arrays.toString(vector);
    }
}
