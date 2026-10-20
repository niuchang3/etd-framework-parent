package org.etd.ai.knowledge.converter;

import org.etd.ai.knowledge.entity.KnowledgeChunkEntity;
import org.etd.ai.knowledge.controller.vo.KnowledgeChunkVO;
import org.mapstruct.Mapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapping;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/** 正文预览对象的编译期转换。 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface KnowledgeChunkConverter {
    /** 无状态转换实例。 */
    KnowledgeChunkConverter INSTANCE = Mappers.getMapper(KnowledgeChunkConverter.class);
    /** @param entity 正文分片 @return 前端预览信息 */
    KnowledgeChunkVO toVO(KnowledgeChunkEntity entity);
    /** @param document 可信资料记录 @return 本轮正文分片的归属和权限副本，不复制资料主键及版本 */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "documentId", source = "id")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "orgId", source = "orgId")
    @Mapping(target = "createBy", source = "createBy")
    @Mapping(target = "processingVersion", source = "processingVersion")
    @Mapping(target = "isPublic", source = "isPublic")
    @Mapping(target = "title", source = "documentName")
    KnowledgeChunkEntity toEntity(KnowledgeDocumentEntity document);
}
