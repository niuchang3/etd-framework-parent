package org.etd.ai.knowledge.converter;
import org.etd.ai.knowledge.model.KnowledgeSearchHit;
import org.etd.ai.knowledge.controller.vo.KnowledgeSearchHitVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;
import org.etd.ai.knowledge.controller.vo.KnowledgeRecallResultVO;
import org.mapstruct.factory.Mappers;
/** 召回结果边界转换。 */
@Mapper
public interface KnowledgeSearchConverter {
    /** 转换实例。 */
    KnowledgeSearchConverter INSTANCE = Mappers.getMapper(KnowledgeSearchConverter.class);
    /** @param hit 内部候选 @return 页面响应 */
    KnowledgeSearchHitVO toVO(KnowledgeSearchHit hit);
    /** @param hits 内部结果 @param elapsedMillis 耗时 @return 实验响应 */
    @Mapping(target = "records", source = "hits")
    @Mapping(target = "total", expression = "java(hits.size())")
    KnowledgeRecallResultVO toResult(List<KnowledgeSearchHit> hits, Long elapsedMillis);
}
