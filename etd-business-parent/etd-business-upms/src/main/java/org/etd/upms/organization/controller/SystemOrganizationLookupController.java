package org.etd.upms.organization.controller;

import org.etd.framework.common.core.model.ResultModel;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.etd.upms.organization.controller.vo.SystemOrganizationVO;
import org.etd.upms.organization.service.SystemOrganizationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 跨菜单的部门回显入口，仅要求登录，不绑定组织管理菜单权限。
 */
@RestController
@RequestMapping("/v1/organization/lookup")
public class SystemOrganizationLookupController {

    private final SystemOrganizationService organizationService;

    /**
     * 注入与组织管理详情共用的查询能力。
     *
     * @param organizationService 组织机构基础查询服务
     */
    public SystemOrganizationLookupController(SystemOrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    /**
     * 按组织机构 ID 查询部门回显信息，沿用框架租户隔离和逻辑删除规则。
     *
     * @param id 需要回显的组织机构 ID
     * @return 与管理详情一致的组织机构信息，不存在时 data 为 null
     */
    @AutoLog("获取部门回显信息")
    @GetMapping("/{id}")
    public ResultModel<SystemOrganizationVO> getOrganization(@PathVariable("id") Long id) {
        SystemOrganizationVO organization = organizationService.selectById(id);
        return ResultModel.success(organization);
    }
}
