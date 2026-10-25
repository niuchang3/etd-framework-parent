package org.etd.ai.knowledge.converter;

import org.etd.ai.knowledge.controller.dto.KnowledgeFolderSaveDTO;
import org.etd.ai.knowledge.controller.vo.KnowledgeFolderVO;
import org.etd.ai.knowledge.entity.KnowledgeFolderEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/** 目录接口对象转换，入参仅映射可编辑字段。 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface KnowledgeFolderConverter {
    /** 编译期生成的无状态转换实例。 */
    KnowledgeFolderConverter INSTANCE = Mappers.getMapper(KnowledgeFolderConverter.class);

    /**
     * 转换页面输入，租户及审计信息交由框架填充。
     * @param dto 已通过接口校验的请求
     * @return 待保存的业务实体
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "knowledgeBaseId", source = "knowledgeBaseId")
    @Mapping(target = "parentId", source = "parentId")
    @Mapping(target = "folderName", expression = "java(dto.getFolderName().trim())")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "sortOrder", source = "sortOrder")
    @Mapping(target = "version", source = "version")
    KnowledgeFolderEntity toEntity(KnowledgeFolderSaveDTO dto);

    /**
     * 转换页面输出，仅暴露响应模型声明的字段。
     * @param entity 业务实体
     * @return 页面响应
     */
    @Mapping(target = "writable", expression = "java(canManageFolder(entity))")
    @Mapping(target = "navigationOnly", expression = "java(!canManageFolder(entity))")
    KnowledgeFolderVO toVO(KnowledgeFolderEntity entity);

    /** @param entity 目录记录 @return 框架角色范围是否允许维护该目录 */
    default boolean canManageFolder(KnowledgeFolderEntity entity) {
        return Boolean.TRUE.equals(entity.getWritable());
    }

    /**
     * 祖先补齐只供导航，不暴露隐藏目录的描述、审计或编辑版本。
     * @param response 已转换的目录响应
     */
    @AfterMapping
    default void hideNavigationDetails(@MappingTarget KnowledgeFolderVO response) {
        if (Boolean.TRUE.equals(response.getNavigationOnly())) {
            response.setDescription(null);
            response.setVersion(null);
            response.setCreateTime(null);
            response.setUpdateTime(null);
        }
    }
}
