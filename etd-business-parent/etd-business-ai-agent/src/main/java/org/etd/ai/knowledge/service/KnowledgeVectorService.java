package org.etd.ai.knowledge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.constant.KnowledgePolicy;
import org.etd.ai.knowledge.entity.*;
import org.etd.ai.knowledge.mapper.KnowledgeVectorMapper;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;
import java.util.*;

/** 向量持久化能力；元数据中的权限值来自资料，不接受前端直接写入。 */
@Service
@RequiredArgsConstructor
public class KnowledgeVectorService {
    /** 向量元数据的租户ID字段，须与DDL生成列保持一致。 */
    private static final String META_TENANT_ID = "tenant_id";
    /** 向量元数据的归属部门ID字段，须与DDL生成列保持一致。 */
    private static final String META_ORG_ID = "org_id";
    /** 向量元数据的租户内公开标记字段，须与DDL生成列保持一致。 */
    private static final String META_IS_PUBLIC = "is_public";
    /** 向量元数据的资料ID字段，须与DDL生成列保持一致。 */
    private static final String META_DOCUMENT_ID = "document_id";
    /** 向量元数据的正文分片ID字段，须与DDL生成列保持一致。 */
    private static final String META_CHUNK_ID = "chunk_id";
    /** 向量元数据的向量模型连接器ID字段，须与DDL生成列保持一致。 */
    private static final String META_MODEL_CONNECTION_ID = "model_connection_id";
    /** 向量元数据的处理轮次字段，须与DDL生成列保持一致。 */
    private static final String META_PROCESSING_VERSION = "processing_version";
    private final KnowledgeVectorMapper mapper;
    private final ObjectMapper json;
    /** @param document 已锁定资料 @param chunk 同轮次正文 @param values 模型输出的向量 */
    public void createVector(KnowledgeDocumentEntity document, KnowledgeChunkEntity chunk, List<Double> values) {
        if (!KnowledgePolicy.DIMENSIONS.contains(document.getEmbedding()) || values.size() != document.getEmbedding()) {
            throw new ApiRuntimeException("模型输出维度与资料配置不一致");
        }
        var metadata = new LinkedHashMap<String, Object>();
        metadata.put(META_TENANT_ID, document.getTenantId().toString());
        metadata.put(META_ORG_ID, document.getOrgId().toString());
        metadata.put(META_IS_PUBLIC, document.getIsPublic());
        metadata.put(META_DOCUMENT_ID, document.getId().toString());
        metadata.put(META_CHUNK_ID, chunk.getId().toString());
        metadata.put(META_MODEL_CONNECTION_ID, document.getEmbeddingModelConnectionId().toString());
        metadata.put(META_PROCESSING_VERSION, document.getProcessingVersion().toString());
        try {
            String encoded = json.writeValueAsString(metadata);
            String vector = json.writeValueAsString(values);
            mapper.insertVector(chunk.getVectorId(), document.getEmbedding(), chunk.getContent(), encoded, vector);
        } catch (Exception exception) {
            throw new ApiRuntimeException("向量数据写入失败");
        }
    }
}
