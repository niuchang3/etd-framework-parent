package org.etd.ai.knowledge.converter;

import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;
import org.etd.ai.knowledge.entity.KnowledgeChunkEntity;
import org.etd.ai.knowledge.model.KnowledgeChunkSource;
import org.etd.ai.knowledge.controller.vo.KnowledgeChunkVO;
import org.etd.ai.knowledge.controller.vo.KnowledgeChunkStateVO;
import org.etd.framework.starter.ai.document.model.DocumentChunk;
import org.mapstruct.*;
import org.etd.framework.starter.ai.document.tokenizer.DocumentTokenCounter;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import org.springframework.ai.document.Document;
import org.etd.ai.knowledge.constant.KnowledgeRetrievalPolicy;
import org.etd.ai.knowledge.controller.dto.KnowledgeChunkEditDTO;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.mapstruct.factory.Mappers;
import org.etd.framework.starter.mybaits.permission.model.DataPermissionScope;

/** 分片边界转换，可信归属只来自已授权资料。 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR, imports = DocumentTokenCounter.class,
        uses = KnowledgeDocumentConverter.class)
public interface KnowledgeChunkConverter {
    /** 编译期生成的无状态转换器。 */
    KnowledgeChunkConverter INSTANCE = Mappers.getMapper(KnowledgeChunkConverter.class);
    /** @param document 已授权资料 @param chunk 引擎分片 @return 待保存实体，继承资料归属及创建人，不复制资料主键和时间审计 */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "isPublic", source = "document.isPublic")
    @Mapping(target = "documentId", source = "document.id")
    @Mapping(target = "createBy", source = "document.createBy")
    @Mapping(target = "tenantId", source = "document.tenantId")
    @Mapping(target = "orgId", source = "document.orgId")
    @Mapping(target = "knowledgeBaseId", source = "document.knowledgeBaseId")
    @Mapping(target = "processingVersion", source = "document.processingVersion")
    @Mapping(target = "chunkIndex", source = "chunk.index")
    @Mapping(target = "content", source = "chunk.rawText")
    @Mapping(target = "source", source = "chunk")
    KnowledgeChunkEntity toEntity(KnowledgeDocumentEntity document, DocumentChunk chunk);

    /**
     * 只选择描述性来源，正文保持原样，是否加入向量输入由官方 MetadataMode 控制。
     * @param document 已授权且已锁定版本的资料
     * @param chunk 当前人工确认的分片，不能使用解析时缓存的旧正文
     * @return 官方文本对象；不含租户、用户、权限字段、页码或内部引用
     */
    @Mapping(target = "id", source = "chunk.id")
    @Mapping(target = "text", source = "chunk.content")
    @Mapping(target = "metadata", expression = "java(embeddingMetadata(document, chunk))")
    @Mapping(target = "media", ignore = true)
    @Mapping(target = "score", ignore = true)
    @Mapping(target = "idGenerator", ignore = true)
    Document toEmbeddingDocument(KnowledgeDocumentEntity document, KnowledgeChunkEntity chunk);

    /** 白名单选择业务语义字段，格式化由官方 Document 完成，不修改权威正文。 */
    default Map<String, Object> embeddingMetadata(KnowledgeDocumentEntity document, KnowledgeChunkEntity chunk) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(KnowledgeRetrievalPolicy.EMBEDDING_DOCUMENT_NAME, document.getDocumentName());
        KnowledgeChunkSource source = chunk.getSource();
        if (!source.headings().isEmpty()) {
            metadata.put(KnowledgeRetrievalPolicy.EMBEDDING_HEADINGS, source.headings());
        }
        if (!source.captions().isEmpty()) {
            metadata.put(KnowledgeRetrievalPolicy.EMBEDDING_CAPTIONS, source.captions());
        }
        return metadata;
    }
    /**
     * @param original 已校验的原分片
     * @param content 第二片的权威正文
     * @return 继承归属、共享、启停及原始来源的新分片，不复制主键、顺序和时间审计
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "documentId", source = "original.documentId")
    @Mapping(target = "tenantId", source = "original.tenantId")
    @Mapping(target = "orgId", source = "original.orgId")
    @Mapping(target = "knowledgeBaseId", source = "original.knowledgeBaseId")
    @Mapping(target = "processingVersion", source = "original.processingVersion")
    @Mapping(target = "createBy", source = "original.createBy")
    @Mapping(target = "isPublic", source = "original.isPublic")
    @Mapping(target = "dataStatus", source = "original.dataStatus")
    @Mapping(target = "source", source = "original.source")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "tokenCount", expression = "java(DocumentTokenCounter.count(content))")
    KnowledgeChunkEntity toSplitEntity(KnowledgeChunkEntity original, String content);

    /** @param chunk 引擎分片 @return 来源集合 */
    KnowledgeChunkSource toSource(DocumentChunk chunk);
    /** @param entity 分片实体 @return 预览信息 */
    @Mapping(target = "writable", expression = "java(canManageChunk(entity))")
    KnowledgeChunkVO toVO(KnowledgeChunkEntity entity);
    /** @param entity 资料实体 @return 工作页状态 */
    @Mapping(target = "writable", expression = "java(KnowledgeDocumentConverter.INSTANCE.canManageDocument(entity))")
    @Mapping(target = "tokenCountTokenizer", expression = "java(DocumentTokenCounter.TOKENIZER)")
    @Mapping(target = "chunkErrorMessage", source = "chunkErrorMessage", qualifiedByName = "safeChunkError")
    KnowledgeChunkStateVO toStateVO(KnowledgeDocumentEntity entity);
    /** @param entity 已授权分片 @return 当前角色范围是否允许维护该分片 */
    default boolean canManageChunk(KnowledgeChunkEntity entity) {
        DataPermissionScope scope = DataPermissionScope.current();
        return scope.canManage(entity.getOrgId(), entity.getCreateBy());
    }
    /** @param inputs 通过字段校验的编辑列表 @return 去重后的编辑值，重复ID明确拒绝 */
    default Map<Long, String> toEditValues(List<KnowledgeChunkEditDTO> inputs) {
        Map<Long, String> values = new LinkedHashMap<>();
        for (KnowledgeChunkEditDTO input : inputs) {
            if (values.putIfAbsent(input.getId(), input.getContent()) != null) {
                throw new ApiRuntimeException("同一分片不能重复提交修改");
            }
        }
        return values;
    }
}
