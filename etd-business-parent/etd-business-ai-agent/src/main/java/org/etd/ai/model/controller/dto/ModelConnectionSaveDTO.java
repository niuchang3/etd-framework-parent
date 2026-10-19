package org.etd.ai.model.controller.dto;

import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.ToString;

import org.etd.ai.model.constant.ModelProtocol;
import org.etd.framework.starter.web.validation.UpdateRequest;

/** ModelConnectionSaveDTO 页面请求；不接收租户、创建人或存储对象键。 */
@Data
public class ModelConnectionSaveDTO {
    /** 所选平台模型，更新不允许切换；换模型应重新建立连接。 */
    @NotNull
    @Positive
    private Long modelId;

    /** 租户选择的接入协议，不是服务商代码。 */
    @NotBlank
    @Pattern(regexp = ModelProtocol.PROTOCOL_PATTERN)
    private String protocol;

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
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ToString.Exclude
    private String apiKey;

    /** 更新时的版本。 */
    @Min(0)
    @NotNull(groups = UpdateRequest.class)
    private Long version;
}
