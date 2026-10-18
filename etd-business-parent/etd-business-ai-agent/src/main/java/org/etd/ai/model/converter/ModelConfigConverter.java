package org.etd.ai.model.converter;

import org.etd.ai.model.controller.dto.ModelConfigSaveDTO;
import org.etd.ai.model.controller.vo.ModelConfigVO;
import org.etd.ai.model.entity.ModelConfigEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/** 模型配置接口对象转换，入参仅映射可编辑字段。 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ModelConfigConverter {
    /** 编译期生成的无状态转换实例。 */
    ModelConfigConverter INSTANCE = Mappers.getMapper(ModelConfigConverter.class);

    /**
     * 转换页面输入，租户及审计信息交由框架填充。
     * @param dto 已通过接口校验的请求
     * @return 待保存的业务实体
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "connectionId", source = "connectionId")
    @Mapping(target = "modelName", expression = "java(dto.getModelName().trim())")
    @Mapping(target = "modelType", source = "modelType")
    @Mapping(target = "modelCode", expression = "java(dto.getModelCode().trim())")
    @Mapping(target = "remark", source = "remark")
    @Mapping(target = "version", source = "version")
    ModelConfigEntity toEntity(ModelConfigSaveDTO dto);

    /**
     * 转换页面输出，仅暴露响应模型声明的字段。
     * @param entity 业务实体
     * @return 页面响应
     */
    @Mapping(target = "verified", constant = "false")
    ModelConfigVO toVO(ModelConfigEntity entity);
}
