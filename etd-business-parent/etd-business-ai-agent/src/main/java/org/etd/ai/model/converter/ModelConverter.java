package org.etd.ai.model.converter;

import org.etd.ai.model.controller.dto.ModelSaveDTO;
import org.etd.ai.model.controller.vo.ModelVO;
import org.etd.ai.model.entity.ModelEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/** 平台模型维护请求及目录响应的编译期转换。 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ModelConverter {
    /** 编译期生成的无状态转换实例。 */
    ModelConverter INSTANCE = Mappers.getMapper(ModelConverter.class);

    /**
     * 只转换允许管理员维护的字段，租户和审计字段由框架控制。
     * @param dto 经过校验的模型请求
     * @return 待保存实体
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "providerCode", expression = "java(dto.getProviderCode().trim())")
    @Mapping(target = "modelName", expression = "java(dto.getModelName().trim())")
    @Mapping(target = "modelCode", expression = "java(dto.getModelCode().trim())")
    @Mapping(target = "modelType", source = "modelType")
    @Mapping(target = "logoPath", source = "logoPath")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "apiKeyUrl", source = "apiKeyUrl")
    @Mapping(target = "setupGuideUrl", source = "setupGuideUrl")
    @Mapping(target = "baseUrlHelp", source = "baseUrlHelp")
    @Mapping(target = "embedding", source = "embedding")
    @Mapping(target = "sort", source = "sort")
    @Mapping(target = "dataStatus", source = "dataStatus")
    @Mapping(target = "version", source = "version")
    ModelEntity toEntity(ModelSaveDTO dto);

    /**
     * 输出模型展示、调用标识和获取凭证的指引。
     * @param entity 平台模型记录
     * @return 模型管理响应
     */
    ModelVO toVO(ModelEntity entity);
}
