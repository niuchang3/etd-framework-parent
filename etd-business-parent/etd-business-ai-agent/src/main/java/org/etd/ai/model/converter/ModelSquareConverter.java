package org.etd.ai.model.converter;

import org.etd.ai.model.controller.vo.ModelSquareModelVO;
import org.etd.ai.model.mapper.result.ModelSquareModel;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/** 广场只读投影到页面响应的编译期转换，不组装表间关联。 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ModelSquareConverter {
    /** 编译期生成的无状态转换实例。 */
    ModelSquareConverter INSTANCE = Mappers.getMapper(ModelSquareConverter.class);

    /**
     * 转换首页模型卡片，不输出租户归属及内部审计属性。
     * @param model 模型定义与连接统计
     * @return 模型选项及接入指引
     */
    ModelSquareModelVO toModelVO(ModelSquareModel model);
}
