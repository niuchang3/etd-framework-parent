package org.etd.ai.model.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.etd.framework.starter.client.core.permission.annotation.Permission;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.etd.ai.model.controller.vo.ModelVO;
import org.etd.ai.model.converter.ModelConverter;
import org.etd.ai.model.service.ModelService;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 平台模型目录入口；租户只读，没有模型新增、修改及删除接口。 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/v1/models")
@Permission("ai:model")
public class ModelController {
    private final ModelService service;

    /**
     * 分页浏览模型目录，可组合筛选服务商与模型类型。
     * @param current 从1开始的页码
     * @param size 每页数量，上限200
     * @param providerCode 可选服务商字典值
     * @param modelType 可选模型类型字典值
     * @return 统一响应中的模型分页
     */
    @AutoLog("分页查询平台模型目录")
    @GetMapping
    public ResultModel<IPage<ModelVO>> page(
            @RequestParam(defaultValue = "1") @Min(1) long current,
            @RequestParam(defaultValue = "10") @Min(1) @Max(200) long size,
            @RequestParam(required = false) String providerCode,
            @RequestParam(required = false) String modelType) {
        var page = service.fetchPage(current, size, providerCode, modelType);
        var result = page.convert(ModelConverter.INSTANCE::toVO);
        return ResultModel.success(result);
    }

    /**
     * 查询模型及接入指引，停用模型不允许新建连接。
     * @param id 平台模型标识
     * @return 模型详情；不存在或停用时返回业务错误
     */
    @AutoLog("获取平台模型详情")
    @GetMapping("/{id}")
    public ResultModel<ModelVO> get(@PathVariable Long id) {
        var model = service.requireModel(id);
        var result = ModelConverter.INSTANCE.toVO(model);
        return ResultModel.success(result);
    }
}
