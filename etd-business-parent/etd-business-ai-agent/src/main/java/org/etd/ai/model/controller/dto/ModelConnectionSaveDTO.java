package org.etd.ai.model.controller.dto;

import jakarta.validation.constraints.*;

import lombok.Data;

import org.etd.ai.model.constant.ModelProtocol;
import org.etd.framework.starter.web.validation.UpdateRequest;

/** ModelConnectionSaveDTO 页面请求；不接收租户、创建人或存储对象键。 */
@Data
public class ModelConnectionSaveDTO {
    /** 连接名称。 */
    @NotBlank
    @Size(max = 100)
    private String connectionName;

    /** 接入协议。 */
    @NotBlank
    @Pattern(regexp = ModelProtocol.PROVIDER_PATTERN)
    private String provider;

    /** API根地址，不得含凭证、查询参数或片段。 */
    @NotBlank
    @Size(max = 1000)
    private String baseUrl;

    /** 认证方式。 */
    @NotBlank
    @Pattern(regexp = ModelProtocol.AUTH_PATTERN)
    private String authType;

    /** 只写API Key，更新为空保留已有密钥。 */
    @Size(max = 4096)
    @lombok.ToString.Exclude
    private String apiKey;

    /** 用途说明。 */
    @Size(max = 500)
    private String remark;

    /** 更新时的版本。 */
    @Min(0)
    @NotNull(groups = UpdateRequest.class)
    private Long version;
}
