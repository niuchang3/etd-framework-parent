package org.etd.ai.model.mapper.result;

import lombok.Data;

/** 已启用向量连接器的选择项，不读取任何认证凭证或服务地址。 */
@Data
public class EmbeddingConnectionOption {
    /** 模型连接器ID。 */
    private Long id;
    /** 平台模型ID。 */
    private Long modelId;
    /** 平台模型显示名称。 */
    private String modelName;
    /** 服务商字典值。 */
    private String providerCode;
    /** 平台登记的支持维度；能力服务根据调用方支持的存储维度裁剪。 */
    private Integer[] embedding;
}
