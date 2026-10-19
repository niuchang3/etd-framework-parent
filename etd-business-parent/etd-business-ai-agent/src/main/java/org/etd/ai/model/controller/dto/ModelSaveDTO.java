package org.etd.ai.model.controller.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.etd.ai.model.constant.ModelType;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.starter.web.validation.UpdateRequest;

/** 平台管理员维护模型管理的请求，不接收租户、创建人等可信字段。 */
@Data
public class ModelSaveDTO {
    /** 管理入口跳转链接仅允许HTTP或HTTPS，不接受脚本协议。 */
    private static final String HTTP_URL_PATTERN = "https?://[^\\s]+";

    /** 服务商字典值，不是接入协议。 */
    @NotBlank
    @Size(max = 64)
    private String providerCode;

    /** 模型显示名称。 */
    @NotBlank
    @Size(max = 100)
    private String modelName;

    /** 调用服务商接口时使用的真实模型代码。 */
    @NotBlank
    @Size(max = 200)
    private String modelCode;

    /** 后端支持的模型主能力类型。 */
    @NotBlank
    @Pattern(regexp = ModelType.PATTERN)
    private String modelType;

    /** 模型Logo资源路径，可清空。 */
    @Size(max = 1000)
    private String logoPath;

    /** 模型用途说明，可清空。 */
    @Size(max = 1000)
    private String description;

    /** 服务商密钥获取页面的HTTP或HTTPS链接，可清空。 */
    @Size(max = 1000)
    @Pattern(regexp = HTTP_URL_PATTERN)
    private String apiKeyUrl;

    /** 接入指南的HTTP或HTTPS链接，可清空。 */
    @Size(max = 1000)
    @Pattern(regexp = HTTP_URL_PATTERN)
    private String setupGuideUrl;

    /** 地址获取和填写说明，可清空。 */
    @Size(max = 1000)
    private String baseUrlHelp;

    /** 平台登记的模型支持维度集合；向量模型必填且元素为不重复的正整数，非向量模型为空；知识库可选值还须与系统支持范围取交集。 */
    private Integer[] embedding;

    /** 目录显示顺序，数值越小越靠前。 */
    @NotNull
    @Min(0)
    private Integer sort;

    /** 启停状态：0停用、1启用；新增不传时由框架填充。 */
    @Min(BasicConstant.DataStatus.DISABLED_CODE)
    @Max(BasicConstant.DataStatus.ENABLED_CODE)
    @NotNull(groups = UpdateRequest.class)
    private Integer dataStatus;

    /** 更新必须回传读取时的版本，防止并发覆盖。 */
    @Min(0)
    @NotNull(groups = UpdateRequest.class)
    private Long version;
}
