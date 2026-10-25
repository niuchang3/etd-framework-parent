package org.etd.ai.knowledge.converter;

import java.util.List;
import org.etd.ai.knowledge.controller.vo.KnowledgeRecallHitVO;
import org.etd.ai.knowledge.model.KnowledgeRecallHit;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/** 召回边界转换，不向接口暴露内部内容轮次。 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface KnowledgeRecallConverter {
    /** 编译生成的无状态转换器。 */
    KnowledgeRecallConverter INSTANCE = Mappers.getMapper(KnowledgeRecallConverter.class);
    /** @param hit 已复核权限及版本的结果 @return 接口命中项 */
    KnowledgeRecallHitVO toVO(KnowledgeRecallHit hit);
    /** @param hits 最终排序结果 @return 接口结果列表 */
    List<KnowledgeRecallHitVO> toVOList(List<KnowledgeRecallHit> hits);
}
