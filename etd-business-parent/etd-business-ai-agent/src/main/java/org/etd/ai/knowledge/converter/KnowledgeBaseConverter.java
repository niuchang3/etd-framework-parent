package org.etd.ai.knowledge.converter;

import org.etd.ai.knowledge.entity.KnowledgeBaseEntity;
import org.etd.ai.knowledge.controller.dto.KnowledgeBaseSaveDTO;
import org.etd.ai.knowledge.controller.vo.KnowledgeBaseVO;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

/** 知识库输入和响应的编译期转换。 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface KnowledgeBaseConverter {
    /** 无状态转换实例。 */
    KnowledgeBaseConverter INSTANCE = Mappers.getMapper(KnowledgeBaseConverter.class);
    /** @param input 已校验设置 @return 仅含用户可编辑字段的实体 */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "version", source = "version")
    @Mapping(target = "orgId", source = "orgId")
    @Mapping(target = "dataStatus", source = "dataStatus")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "isPublic", source = "isPublic")
    @Mapping(target = "retrievalMode", source = "retrievalMode")
    @Mapping(target = "keywordRecallSize", source = "keywordRecallSize")
    @Mapping(target = "vectorRecallSize", source = "vectorRecallSize")
    @Mapping(target = "topK", source = "topK")
    @Mapping(target = "similarityThreshold", source = "similarityThreshold")
    @Mapping(target = "contextWindowSize", source = "contextWindowSize")
    @Mapping(target = "rerankEnabled", source = "rerankEnabled")
    @Mapping(target = "rerankConnectionId", source = "rerankConnectionId")
    @Mapping(target = "rerankThreshold", source = "rerankThreshold")
    @Mapping(target = "embeddingConnectionId", source = "embeddingConnectionId")
    @Mapping(target = "dimensions", source = "dimensions")
    KnowledgeBaseEntity toEntity(KnowledgeBaseSaveDTO input);
    /** @param entity 已授权知识库 @return 脱敏响应 */
    KnowledgeBaseVO toVO(KnowledgeBaseEntity entity);
    /** @param input 仅含可编辑字段的设置 @param target 已锁定实体，不覆盖索引版本 */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "orgId", source = "orgId")
    @Mapping(target = "dataStatus", source = "dataStatus")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "isPublic", source = "isPublic")
    @Mapping(target = "retrievalMode", source = "retrievalMode")
    @Mapping(target = "keywordRecallSize", source = "keywordRecallSize")
    @Mapping(target = "vectorRecallSize", source = "vectorRecallSize")
    @Mapping(target = "topK", source = "topK")
    @Mapping(target = "similarityThreshold", source = "similarityThreshold")
    @Mapping(target = "contextWindowSize", source = "contextWindowSize")
    @Mapping(target = "rerankEnabled", source = "rerankEnabled")
    @Mapping(target = "rerankConnectionId", source = "rerankConnectionId")
    @Mapping(target = "rerankThreshold", source = "rerankThreshold")
    @Mapping(target = "embeddingConnectionId", source = "embeddingConnectionId")
    @Mapping(target = "dimensions", source = "dimensions")
    void updateEntity(KnowledgeBaseEntity input, @MappingTarget KnowledgeBaseEntity target);
}
