package org.etd.upms.dict.controller;

import com.etd.framework.starter.client.core.permission.annotation.Permission;
import org.etd.upms.menu.constant.MenuPermissionCode;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.upms.dict.biz.SystemDictBizService;
import org.etd.upms.dict.controller.dto.SystemDictTypeSaveDTO;
import org.etd.upms.dict.controller.vo.SystemDictDataVO;
import org.etd.upms.dict.controller.vo.SystemDictTypeVO;
import org.etd.upms.dict.service.SystemDictDataService;
import org.etd.upms.dict.service.SystemDictTypeService;
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
import java.util.Map;

/**
 * 字典类型管理 Controller 控制器入口。
 */
@Validated
@Permission(MenuPermissionCode.DICT)
@RestController
@RequestMapping("/v1/dict/type")
public class SystemDictTypeController {

    @Autowired
    private SystemDictTypeService dictTypeService;

    @Autowired
    private SystemDictDataService dictDataService;

    @Autowired
    private SystemDictBizService dictBizService;

    @GetMapping
    public ResultModel<IPage<SystemDictTypeVO>> page(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于1") long current,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数不能小于1")
            @Max(value = 200, message = "每页条数不能超过200") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled) {
        return ResultModel.success(dictTypeService.page(current, size, keyword, enabled));
    }

    /**
     * detail
     *
     * @param id 参数 id
     * @return 处理结果
     */
    @GetMapping("/{id}")
    public ResultModel<SystemDictTypeVO> detail(@PathVariable Long id) {
        return ResultModel.success(dictTypeService.selectById(id));
    }

    @GetMapping("/data")
    public ResultModel<Map<String, List<SystemDictDataVO>>> enabledData(
            @RequestParam("typeCodes") @NotEmpty(message = "字典类型编码不能为空")
            List<@NotBlank(message = "字典类型编码不能为空")
                    @Size(max = 100, message = "字典类型编码不能超过100个字符") String> typeCodes) {
        return ResultModel.success(dictBizService.mapEnabledDataByTypeCodes(typeCodes));
    }

    /**
     * 新增保存
     *
     * @param dto 参数 dto
     * @return 处理结果
     */
    @PostMapping
    public ResultModel<Long> insert(@Valid @RequestBody SystemDictTypeSaveDTO dto) {
        return ResultModel.success(dictTypeService.insert(dto));
    }

    /**
     * 更新修改
     *
     * @param id 参数 id
     * @param dto 参数 dto
     * @return 处理结果
     */
    @PutMapping("/{id}")
    public ResultModel<Boolean> update(@PathVariable Long id, @Valid @RequestBody SystemDictTypeSaveDTO dto) {
        return ResultModel.success(dictTypeService.update(id, dto));
    }

    /**
     * 删除
     *
     * @param id 参数 id
     * @return 处理结果
     */
    @DeleteMapping("/{id}")
    public ResultModel<Boolean> delete(@PathVariable Long id) {
        return ResultModel.success(dictBizService.deleteType(id));
    }

    /**
     * 切换 Enabled
     *
     * @param id 参数 id
     * @param enabled 参数 enabled
     * @return 处理结果
     */
    @PatchMapping("/{id}/enabled/{enabled}")
    public ResultModel<Boolean> switchEnabled(@PathVariable Long id, @PathVariable Boolean enabled) {
        return ResultModel.success(dictTypeService.switchEnabled(id, enabled));
    }
}
