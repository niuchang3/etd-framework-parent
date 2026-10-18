package org.etd.ai.model.converter;

import org.etd.ai.model.controller.dto.ModelConnectionSaveDTO;
import org.etd.ai.model.controller.vo.ModelConnectionVO;
import org.etd.ai.model.entity.ModelConnectionEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/** 模型连接接口对象转换，入参仅映射可编辑字段。 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ModelConnectionConverter {
    /** 编译期生成的无状态转换实例。 */
    ModelConnectionConverter INSTANCE = Mappers.getMapper(ModelConnectionConverter.class);

    /**
     * 转换页面输入，租户及审计信息交由框架填充。
     * @param dto 已通过接口校验的请求
     * @return 待保存的业务实体
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "connectionName", expression = "java(dto.getConnectionName().trim())")
    @Mapping(target = "provider", source = "provider")
    @Mapping(target = "baseUrl", source = "baseUrl")
    @Mapping(target = "authType", source = "authType")
    @Mapping(target = "remark", source = "remark")
    @Mapping(target = "version", source = "version")
    ModelConnectionEntity toEntity(ModelConnectionSaveDTO dto);

    /**
     * 转换页面输出，仅暴露响应模型声明的字段。
     * @param entity 业务实体
     * @return 页面响应
     */
    @Mapping(target = "credentialConfigured", expression = "java(entity.getApiKeyCiphertext() != null)")
    ModelConnectionVO toVO(ModelConnectionEntity entity);
}
