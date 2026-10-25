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
    @Mapping(target = "writable", expression = "java(canManageDocument(entity))")
    @Mapping(target = "chunkErrorMessage", source = "chunkErrorMessage", qualifiedByName = "safeChunkError")
    KnowledgeDocumentListVO toListVO(KnowledgeDocumentEntity entity);
    /** @param entity 资料记录 @return 脱敏资料详情 */
    @Mapping(target = "writable", expression = "java(canManageDocument(entity))")
    @Mapping(target = "chunkErrorMessage", source = "chunkErrorMessage", qualifiedByName = "safeChunkError")
    KnowledgeDocumentVO toVO(KnowledgeDocumentEntity entity);
    /** @param dto 新上传请求 @return 仅包含允许输入字段的记录 */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "isPublic", source = "isPublic")
    @Mapping(target = "folderId", source = "folderId")
    @Mapping(target = "knowledgeBaseId", source = "knowledgeBaseId")
    @Mapping(target = "originalFilename", source = "originalFilename")
    @Mapping(target = "contentType", source = "contentType")
    @Mapping(target = "fileSize", source = "fileSize")
    @Mapping(target = "fileSha256", source = "fileSha256")
    KnowledgeDocumentEntity toEntity(KnowledgeUploadInitDTO dto);
    /** @param source 经校验的编辑请求 @param target 已加载并锁定的记录 */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "isPublic", source = "isPublic")
    @Mapping(target = "folderId", source = "folderId")
    @Mapping(target = "documentName", source = "documentName")
    @Mapping(target = "description", source = "description")
    void updateEntity(KnowledgeDocumentEntity source, @MappingTarget KnowledgeDocumentEntity target);
    /** @param dto 资料配置请求 @return 可编辑字段集合 */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "isPublic", source = "isPublic")
    @Mapping(target = "folderId", source = "folderId")
    @Mapping(target = "documentName", source = "documentName")
    @Mapping(target = "description", source = "description")
    KnowledgeDocumentEntity toEntity(KnowledgeDocumentSaveDTO dto);

    /** @param message 服务端保存的业务错误摘要 @return 去除内部解析引擎名称的用户提示，保留时限及进度 */
    @Named("safeChunkError")
    static String toSafeChunkErrorMessage(String message) {
        if (message == null) {
            return null;
        }
        return message.replaceAll("(?i)docling(?:服务)?", "文档解析服务");
    }

    /** @param entity 已授权读取的资料 @return 当前角色范围是否允许维护该资料 */
    default boolean canManageDocument(KnowledgeDocumentEntity entity) {
        return Boolean.TRUE.equals(entity.getWritable());
    }
}
