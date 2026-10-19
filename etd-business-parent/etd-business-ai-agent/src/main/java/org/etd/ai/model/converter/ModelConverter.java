package org.etd.ai.model.converter;

import org.etd.ai.model.controller.vo.ModelVO;
import org.etd.ai.model.entity.ModelEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/** 平台模型实体转换为租户只读目录响应。 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ModelConverter {
    /** 编译期生成的无状态转换实例。 */
    ModelConverter INSTANCE = Mappers.getMapper(ModelConverter.class);

    /**
     * 输出模型展示、调用标识和获取凭证的指引。
     * @param entity 平台模型记录
     * @return 模型目录响应
     */
    ModelVO toVO(ModelEntity entity);
}
