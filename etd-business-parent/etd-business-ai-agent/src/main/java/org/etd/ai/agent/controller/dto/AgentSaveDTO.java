package org.etd.ai.agent.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.etd.ai.agent.model.AgentConfiguration;
import org.etd.framework.starter.web.validation.UpdateRequest;

/** 创建及完整替换Agent草稿的请求，不接收租户、发布配置或审计身份。 */
@Data
public class AgentSaveDTO {
    /** 所属组织，创建后不允许修改。 */
    @NotNull
    @Positive
    private Long orgId;
    /** Agent名称，非空白且不超过100字符。 */
    @NotBlank
    @Size(max = 100)
    private String name;
    /** Agent简介，省略或null表示清空。 */
    @Size(max = 1000)
    private String description;
    /** 头像资源路径，省略或null表示清空。 */
    @Size(max = 1000)
    private String avatarPath;
    /** 是否租户内公开使用，不扩大维护及知识访问权限。 */
    @NotNull
    private Boolean isPublic;
    /** 草稿配置，允许提交空对象，嵌套字段仍执行类型和边界校验。 */
    @Valid
    @NotNull
    private AgentConfiguration draftConfig;
    /** 修改时必填的乐观锁版本；创建不接收自定义初始版本。 */
    @Min(0)
    @NotNull(groups = UpdateRequest.class)
    private Long version;
}
