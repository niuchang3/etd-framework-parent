package org.etd.ai.knowledge.converter;

import org.etd.ai.knowledge.controller.dto.KnowledgeDocumentSaveDTO;
import org.etd.ai.knowledge.controller.dto.KnowledgeUploadInitDTO;
import org.etd.ai.knowledge.controller.vo.KnowledgeDocumentVO;
import org.etd.ai.knowledge.controller.vo.KnowledgeDocumentListVO;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

/** 资料接口边界转换，不允许请求覆盖可信身份与内部状态。 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface KnowledgeDocumentConverter {
    /** 编译期生成的无状态转换器。 */
    KnowledgeDocumentConverter INSTANCE = Mappers.getMapper(KnowledgeDocumentConverter.class);
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
    void updateEntity(KnowledgeDocumentEntity source, @MappingTarget KnowledgeDocumentEntity target);
    /** @param dto 资料配置请求 @return 可编辑字段集合 */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "folderId", source = "folderId")
    @Mapping(target = "documentName", source = "documentName")
    @Mapping(target = "description", source = "description")
    KnowledgeDocumentEntity toEntity(KnowledgeDocumentSaveDTO dto);
}
