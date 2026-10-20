package org.etd.ai.knowledge.converter;

import org.etd.ai.knowledge.controller.dto.KnowledgeDocumentSaveDTO;
import org.etd.ai.knowledge.controller.dto.KnowledgeUploadInitDTO;
import org.etd.ai.knowledge.controller.vo.KnowledgeDocumentVO;
import org.etd.ai.knowledge.controller.vo.KnowledgeDocumentListVO;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;
import org.mapstruct.*;
import org.etd.ai.model.mapper.result.EmbeddingConnectionOption;
import org.etd.ai.knowledge.controller.vo.KnowledgeEmbeddingOptionVO;
import org.mapstruct.factory.Mappers;

/** 资料接口边界转换，不允许请求覆盖可信身份与内部状态。 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface KnowledgeDocumentConverter {
    /** 编译期生成的无状态转换器。 */
    KnowledgeDocumentConverter INSTANCE = Mappers.getMapper(KnowledgeDocumentConverter.class);
    /** @param option 无凭证的向量连接器选择项 @return 资料配置页面选择项 */
    KnowledgeEmbeddingOptionVO toVO(EmbeddingConnectionOption option);
    /** @param entity 资料记录 @return 不含分片回执及内部配置的列表摘要 */
    KnowledgeDocumentListVO toListVO(KnowledgeDocumentEntity entity);
    /** @param entity 资料记录 @return 脱敏资料详情 */
    KnowledgeDocumentVO toVO(KnowledgeDocumentEntity entity);
    /** @param dto 新上传请求 @return 仅包含允许输入字段的记录 */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "folderId", source = "folderId")
    @Mapping(target = "orgId", source = "orgId")
    @Mapping(target = "originalFilename", source = "originalFilename")
    @Mapping(target = "contentType", source = "contentType")
    @Mapping(target = "fileSize", source = "fileSize")
    @Mapping(target = "fileSha256", source = "fileSha256")
    KnowledgeDocumentEntity toEntity(KnowledgeUploadInitDTO dto);
    /** @param source 经校验的编辑请求 @param target 已加载并锁定的记录 */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "folderId", source = "folderId")
    @Mapping(target = "documentName", source = "documentName")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "isPublic", source = "isPublic")
    @Mapping(target = "retrievalMode", source = "retrievalMode")
    @Mapping(target = "embeddingModelConnectionId", source = "embeddingModelConnectionId")
    @Mapping(target = "embedding", source = "embedding")
    @Mapping(target = "chunkMaxTokens", source = "chunkMaxTokens")
    @Mapping(target = "chunkOverlapTokens", source = "chunkOverlapTokens")
    @Mapping(target = "keywordTopK", source = "keywordTopK")
    @Mapping(target = "vectorTopK", source = "vectorTopK")
    @Mapping(target = "retrievalTopK", source = "retrievalTopK")
    @Mapping(target = "similarityThreshold", source = "similarityThreshold")
    void updateEntity(KnowledgeDocumentEntity source, @MappingTarget KnowledgeDocumentEntity target);
    /** @param dto 资料配置请求 @return 可编辑字段集合 */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "folderId", source = "folderId")
    @Mapping(target = "documentName", source = "documentName")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "isPublic", source = "isPublic")
    @Mapping(target = "retrievalMode", source = "retrievalMode")
    @Mapping(target = "embeddingModelConnectionId", source = "embeddingModelConnectionId")
    @Mapping(target = "embedding", source = "embedding")
    @Mapping(target = "chunkMaxTokens", source = "chunkMaxTokens")
    @Mapping(target = "chunkOverlapTokens", source = "chunkOverlapTokens")
    @Mapping(target = "keywordTopK", source = "keywordTopK")
    @Mapping(target = "vectorTopK", source = "vectorTopK")
    @Mapping(target = "retrievalTopK", source = "retrievalTopK")
    @Mapping(target = "similarityThreshold", source = "similarityThreshold")
    KnowledgeDocumentEntity toEntity(KnowledgeDocumentSaveDTO dto);
}
