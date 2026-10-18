package org.etd.ai.knowledge.converter;

import org.etd.ai.knowledge.controller.dto.KnowledgeFolderSaveDTO;
import org.etd.ai.knowledge.controller.vo.KnowledgeFolderVO;
import org.etd.ai.knowledge.entity.KnowledgeFolderEntity;
import org.mapstruct.BeanMapping;
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
    @Mapping(target = "writable", constant = "true")
    @Mapping(target = "navigationOnly", constant = "false")
    KnowledgeFolderVO toVO(KnowledgeFolderEntity entity);
}
