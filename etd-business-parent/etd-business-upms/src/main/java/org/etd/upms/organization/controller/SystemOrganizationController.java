package org.etd.upms.organization.controller;

import jakarta.validation.Valid;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.upms.organization.biz.SystemOrganizationBizService;
import org.etd.upms.organization.controller.dto.SystemOrganizationSaveDTO;
import org.etd.upms.organization.controller.vo.SystemOrganizationVO;
import org.etd.upms.organization.service.SystemOrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 组织机构 Controller 入口。
 */
@Validated
@RestController
@RequestMapping("/v1/organization")
public class SystemOrganizationController {

    @Autowired
    private SystemOrganizationService organizationService;

    @Autowired
    private SystemOrganizationBizService organizationBizService;

    /**
     * 查询组织机构树列表
     */
    @GetMapping("/tree")
    public ResultModel<List<SystemOrganizationVO>> tree(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "enabled", required = false) Boolean enabled) {
        return ResultModel.success(organizationBizService.selectOrganizationTreeList(keyword, enabled));
    }

    /**
     * detail
     *
     * @param @PathVariable("id" 参数 @PathVariable("id"
     * @return 处理结果
     */
    @GetMapping("/{id}")
    public ResultModel<SystemOrganizationVO> detail(@PathVariable("id") Long id) {
        return ResultModel.success(organizationService.selectById(id));
    }

    /**
     * 新增保存
     *
     * @param dto 参数 dto
     * @return 处理结果
     */
    @PostMapping
    public ResultModel<Long> insert(@Valid @RequestBody SystemOrganizationSaveDTO dto) {
        return ResultModel.success(organizationBizService.insert(dto));
    }

    /**
     * 更新修改
     *
     * @param @PathVariable("id" 参数 @PathVariable("id"
     * @return 处理结果
     */
    @PutMapping("/{id}")
    public ResultModel<Boolean> update(@PathVariable("id") Long id,
                                       @Valid @RequestBody SystemOrganizationSaveDTO dto) {
        return ResultModel.success(organizationBizService.update(id, dto));
    }

    /**
     * 删除
     *
     * @param @PathVariable("id" 参数 @PathVariable("id"
     * @return 处理结果
     */
    @DeleteMapping("/{id}")
    public ResultModel<Boolean> delete(@PathVariable("id") Long id) {
        return ResultModel.success(organizationBizService.delete(id));
    }

    /**
     * 切换 Enabled
     *
     * @param @PathVariable("id" 参数 @PathVariable("id"
     * @return 处理结果
     */
    @PatchMapping("/{id}/enabled/{enabled}")
    public ResultModel<Boolean> switchEnabled(@PathVariable("id") Long id,
                                               @PathVariable("enabled") Boolean enabled) {
        return ResultModel.success(organizationService.switchEnabled(id, enabled));
    }
}
