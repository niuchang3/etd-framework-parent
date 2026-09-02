package org.etd.upms.organization.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 组织机构保存与更新 DTO。
 */
@Data
public class SystemOrganizationSaveDTO {

    private Long parentId;

    @NotBlank(message = "组织编码不能为空")
    @Size(max = 100, message = "组织编码不能超过100个字符")
    private String orgCode;

    @NotBlank(message = "组织名称不能为空")
    @Size(max = 100, message = "组织名称不能超过100个字符")
    private String orgName;

    @Size(max = 50, message = "组织类型不能超过50个字符")
    private String orgType;

    private Long leaderUserId;

    private Integer sort;
}
