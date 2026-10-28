package org.etd.ai.model.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.etd.framework.starter.client.core.permission.annotation.Permission;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.RequiredArgsConstructor;

import org.etd.ai.model.biz.ModelBizService;
import org.etd.common.permission.constant.ModelPermissionCode;
import org.etd.ai.model.controller.dto.ModelSaveDTO;
import org.etd.ai.model.controller.vo.ModelVO;
import org.etd.ai.model.converter.ModelConverter;
import org.etd.ai.model.service.ModelService;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.etd.framework.starter.web.validation.UpdateRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.etd.ai.model.entity.ModelEntity;

/** 平台模型管理入口；管理员增删改查，租户仅浏览已启用模型。 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/v1/models")
@Permission(ModelPermissionCode.MODEL)
public class ModelController {
    private final ModelService service;
    private final ModelBizService biz;

    /**
     * 分页浏览模型管理，可组合筛选服务商与模型类型。
     * @param current 从1开始的页码
     * @param size 每页数量，上限200
     * @param providerCode 可选服务商字典值
     * @param modelType 可选模型类型字典值
     * @param dataStatus 管理员可选状态筛选；普通租户始终只查看已启用模型
     * @return 统一响应中的模型分页
     */
    @AutoLog("分页查询平台模型管理")
    @GetMapping
    public ResultModel<IPage<ModelVO>> page(
            @RequestParam(defaultValue = "1") @Min(1) long current,
            @RequestParam(defaultValue = "10") @Min(1) @Max(200) long size,
            @RequestParam(required = false) String providerCode,
            @RequestParam(required = false) String modelType,
            @RequestParam(required = false)
            @Min(BasicConstant.DataStatus.DISABLED_CODE)
            @Max(BasicConstant.DataStatus.ENABLED_CODE) Integer dataStatus) {
        IPage<ModelEntity> page = service.fetchPage(current, size, providerCode, modelType, dataStatus);
        IPage<ModelVO> result = page.convert(ModelConverter.INSTANCE::toVO);
        return ResultModel.success(result);
    }

    /**
     * 查询模型及接入指引；平台管理员可以查看停用模型。
     * @param id 平台模型标识
     * @return 模型详情；普通租户不能查看停用模型
     */
    @AutoLog("获取平台模型详情")
    @GetMapping("/{id}")
    public ResultModel<ModelVO> get(@PathVariable Long id) {
        ModelEntity model = service.fetchModel(id);
        ModelVO result = ModelConverter.INSTANCE.toVO(model);
        return ResultModel.success(result);
    }

    /**
     * 新增平台模型，仅平台管理员可写。
     * @param dto 模型定义，不包含租户或审计字段
     * @return 新模型标识
     */
    @AutoLog("新增平台模型")
    @PostMapping
    public ResultModel<Long> save(@Valid @RequestBody ModelSaveDTO dto) {
        ModelEntity entity = ModelConverter.INSTANCE.toEntity(dto);
        Long id = service.createModel(entity);
        return ResultModel.success(id);
    }

    /**
     * 按版本修改平台模型；已使用模型的技术身份不可改变。
     * @param id 平台模型标识
     * @param dto 完整模型定义及当前版本，选填属性可显式清空
     * @return 修改成功标识
     */
    @AutoLog("修改平台模型")
    @PutMapping("/{id}")
    public ResultModel<Boolean> update(@PathVariable Long id,
            @Validated(UpdateRequest.class) @RequestBody ModelSaveDTO dto) {
        ModelEntity entity = ModelConverter.INSTANCE.toEntity(dto);
        entity.setId(id);
        biz.modifyModel(entity);
        return ResultModel.success(true);
    }

    /**
     * 删除未被连接器使用的平台模型，仅平台管理员可写。
     * @param id 模型标识
     * @return 删除成功标识
     */
    @AutoLog("删除平台模型")
    @DeleteMapping("/{id}")
    public ResultModel<Boolean> remove(@PathVariable Long id) {
        biz.removeModel(id);
        return ResultModel.success(true);
    }
}
