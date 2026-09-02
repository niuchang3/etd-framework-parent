package org.etd.upms.user.controller.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户关联组织信息视图响应对象 VO。
 */
@Data
public class SystemUserOrganizationVO implements Serializable {

    private static final long serialVersionUID = 6640253130557339152L;

    private Long id;

    private Long tenantId;

    private Long userId;

    private Long organizationId;

    private String organizationName;

    private Boolean primaryOrganization;
}
