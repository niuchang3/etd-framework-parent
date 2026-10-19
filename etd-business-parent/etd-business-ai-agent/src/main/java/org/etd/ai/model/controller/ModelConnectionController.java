package org.etd.ai.model.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.etd.framework.starter.client.core.permission.annotation.Permission;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import lombok.RequiredArgsConstructor;

import org.etd.ai.model.biz.ModelBizService;
import org.etd.ai.model.controller.dto.ModelConnectionSaveDTO;
import org.etd.ai.model.controller.vo.ModelConnectionVO;
import org.etd.ai.model.converter.ModelConnectionConverter;
import org.etd.ai.model.service.ModelConnectionService;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.etd.framework.starter.web.validation.UpdateRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 租户模型连接页面，只保存配置，不提供模型调用。 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/v1/model-connections")
@Permission("ai:model-connection")
public class ModelConnectionController {
    private final ModelConnectionService service;
    private final ModelBizService biz;

    /**
     * 分页返回脱敏页面数据。
     *
     * @param current 从1开始的页码
     * @param size 每页记录数或预期字节数，以方法用途为准
     * @param modelId 可选平台模型标识
     * @param dataStatus 可选启停状态：0停用、1启用；不传表示全部状态
     * @return 统一业务响应；code=2000表示成功，data语义见方法说明
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @AutoLog("分页查询模型连接列表")
    @GetMapping
    public ResultModel<IPage<ModelConnectionVO>> page(
            @RequestParam(defaultValue = "1") @Min(1) long current,
            @RequestParam(defaultValue = "10") @Min(1) @Max(200) long size,
            @RequestParam(required = false)
                    @Min(BasicConstant.DataStatus.DISABLED_CODE)
                    @Max(BasicConstant.DataStatus.ENABLED_CODE)
                    Integer dataStatus,
            @RequestParam(required = false) @Positive Long modelId) {
        var page = service.fetchPage(current, size, modelId, dataStatus);
        var result = page.convert(ModelConnectionConverter.INSTANCE::toVO);
        return ResultModel.success(result);
    }

    /**
     * 取得同租户配置详情。
     *
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @return 统一业务响应；code=2000表示成功，data语义见方法说明
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @AutoLog("获取模型连接详情")
    @GetMapping("/{id}")
    public ResultModel<ModelConnectionVO> get(@PathVariable Long id) {
        var entity = service.fetchConnection(id);
        var result = ModelConnectionConverter.INSTANCE.toVO(entity);
        return ResultModel.success(result);
    }

    /**
     * 保存草稿配置，不发起外部请求。
     *
     * @param dto 经过接口参数校验的页面请求，不包含可信身份字段
     * @return 统一业务响应；code=2000表示成功，data语义见方法说明
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @AutoLog("新增模型连接")
    @PostMapping
    public ResultModel<Long> save(@Valid @RequestBody ModelConnectionSaveDTO dto) {
        var e = ModelConnectionConverter.INSTANCE.toEntity(dto);
        var id = biz.createConnection(e, dto.getApiKey());
        return ResultModel.success(id);
    }

    /**
     * 按版本更新配置，密钥从不作为响应回显。
     *
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @param dto 经过接口参数校验的页面请求，不包含可信身份字段
     * @return 统一业务响应；code=2000表示成功，data语义见方法说明
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @AutoLog("更新模型连接")
    @PutMapping("/{id}")
    public ResultModel<Boolean> update(
            @PathVariable Long id,
            @Validated(UpdateRequest.class) @RequestBody ModelConnectionSaveDTO dto) {
        var e = ModelConnectionConverter.INSTANCE.toEntity(dto);
        e.setId(id);
        biz.modifyConnection(e, dto.getApiKey(), dto.getVersion());
        return ResultModel.success(true);
    }

    /**
     * 删除配置，使用中的连接禁止删除。
     *
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @return 统一业务响应；code=2000表示成功，data语义见方法说明
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @AutoLog("删除模型连接")
    @DeleteMapping("/{id}")
    public ResultModel<Boolean> remove(@PathVariable Long id) {
        service.removeConnection(id);
        return ResultModel.success(true);
    }

}
