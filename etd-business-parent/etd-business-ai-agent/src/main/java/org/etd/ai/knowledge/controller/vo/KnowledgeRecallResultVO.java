package org.etd.ai.knowledge.controller.vo;
import lombok.Data;
import java.util.List;
/** 召回实验结果及本次后端耗时，不包含模型凭证或底层向量。 */
@Data
public class KnowledgeRecallResultVO {
    /** 最终命中分片列表，未匹配时为空。 */
    private List<KnowledgeSearchHitVO> records;
    /** 本次实际返回数量。 */
    private Integer total;
    /** 检索和重排总耗时，单位毫秒。 */
    private Long elapsedMillis;
}
