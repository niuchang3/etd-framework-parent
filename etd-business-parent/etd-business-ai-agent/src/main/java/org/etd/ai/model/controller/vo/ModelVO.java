package org.etd.ai.model.controller.vo;

import lombok.Data;

/** 租户选模型时使用的公共目录信息，不包含任何租户凭证。 */
@Data
public class ModelVO {
    /** 平台模型标识，按框架约定以字符串传输。 */
    private Long id;
    /** 服务商字典值，用于筛选，不表示接入协议。 */
    private String providerCode;

    /** 平台维护的模型显示名称。 */
    private String modelName;

    /** 请求服务商时使用的模型代码，由平台维护。 */
    private String modelCode;

    /** 模型主能力类型，对应模型类型字典。 */
    private String modelType;

    /** 模型图标资源路径，不包含租户凭证。 */
    private String logoPath;

    /** 模型用途介绍。 */
    private String description;

    /** 服务商密钥获取页面，不是模型API地址。 */
    private String apiKeyUrl;

    /** 模型开通及接入指南地址。 */
    private String setupGuideUrl;

    /** 服务地址获取及填写说明，专属实例地址由租户提供。 */
    private String baseUrlHelp;

    /** 平台定义的固定向量维度；未确定时为空，不能作为连接已验证的证明。 */
    private Integer embeddingDimensions;

    /** 模型目录展示顺序，数值越小越靠前。 */
    private Integer sort;
}
