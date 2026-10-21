package org.etd.ai.model.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.etd.framework.starter.client.core.permission.annotation.Permission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.etd.ai.model.biz.ModelSquareBizService;
import org.etd.common.permission.constant.ModelPermissionCode;
import org.etd.ai.model.constant.ModelType;
import org.etd.ai.model.controller.dto.ModelConnectionSaveDTO;
import org.etd.ai.model.controller.dto.ModelConnectionStatusDTO;
import org.etd.ai.model.controller.vo.ModelConnectionVO;
import org.etd.ai.model.controller.vo.ModelSquareModelVO;
import org.etd.ai.model.controller.vo.ModelVO;
import org.etd.ai.model.converter.ModelConnectionConverter;
import org.etd.ai.model.converter.ModelConverter;
import org.etd.ai.model.converter.ModelSquareConverter;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
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

/** 首页模型广场聚合入口，模型查询及接入管理统一使用广场菜单的read/write权限。 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/v1/model-square")
@Permission(ModelPermissionCode.SQUARE)
public class ModelSquareController {
    private final ModelSquareBizService biz;

    /**
     * 平铺查询首页模型卡片，每条记录对应一个平台已启用模型。
     * @param current 从1开始的页码
     * @param size 每页数量，上限200
     * @param providerCode 可选服务商字典值
     * @param modelType 可选模型主能力类型
     * @param keyword 可选模型名称或调用代码搜索文本
     * @param connected true仅已配置、false仅未配置，省略返回全部已启用模型
     * @return 模型分页及接入指引，已有连接统计遵循框架访问范围
     */
    @AutoLog("分页查询模型广场模型列表")
    @GetMapping("/models")
    public ResultModel<IPage<ModelSquareModelVO>> pageModels(
            @RequestParam(defaultValue = "1") @Min(1) long current,
            @RequestParam(defaultValue = "12") @Min(1) @Max(200) long size,
            @RequestParam(required = false) @Size(max = 64) String providerCode,
            @RequestParam(required = false) @Pattern(regexp = ModelType.PATTERN) String modelType,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) Boolean connected) {
        var page = biz.fetchModelPage(current, size, providerCode, modelType, keyword, connected);
        var result = page.convert(ModelSquareConverter.INSTANCE::toModelVO);
        return ResultModel.success(result);
    }

    /**
     * 在广场内查看模型详情及接入指引，无需模型管理菜单权限。
     * @param id 平台模型标识
     * @return 已启用模型详情
     * @throws ApiRuntimeException 模型不存在、已删除或已停用
     */
    @AutoLog("查询模型广场模型详情")
    @GetMapping("/models/{id}")
    public ResultModel<ModelVO> getModel(@PathVariable @Positive Long id) {
        var model = biz.fetchModel(id);
        var result = ModelConverter.INSTANCE.toVO(model);
        return ResultModel.success(result);
    }

    /**
     * 在广场内分页查询已有连接，包含停用模型的连接。
     * @param current 从1开始的页码
     * @param size 每页数量，上限200
     * @param modelId 可选平台模型标识
     * @param dataStatus 可选连接启停状态：0停用、1启用
     * @param providerCode 可选服务商字典值
     * @return 当前访问范围内的脱敏连接分页
     */
    @AutoLog("分页查询模型广场连接列表")
    @GetMapping("/connections")
    public ResultModel<IPage<ModelConnectionVO>> pageConnections(
            @RequestParam(defaultValue = "1") @Min(1) long current,
            @RequestParam(defaultValue = "10") @Min(1) @Max(200) long size,
            @RequestParam(required = false) @Positive Long modelId,
            @RequestParam(required = false) @Min(BasicConstant.DataStatus.DISABLED_CODE)
            @Max(BasicConstant.DataStatus.ENABLED_CODE) Integer dataStatus,
            @RequestParam(required = false) @Size(max = 64) String providerCode) {
        var page = biz.fetchConnectionPage(current, size, modelId, dataStatus, providerCode);
        var result = page.convert(ModelConnectionConverter.INSTANCE::toVO);
        return ResultModel.success(result);
    }

    /**
     * 查看连接配置，响应不包含API Key明文、密文和内部密钥标识。
     * @param id 连接标识
     * @return 当前访问范围内的脱敏连接详情
     * @throws ApiRuntimeException 连接不存在或不在当前访问范围
     */
    @AutoLog("查询模型广场连接详情")
    @GetMapping("/connections/{id}")
    public ResultModel<ModelConnectionVO> getConnection(@PathVariable @Positive Long id) {
        var connection = biz.fetchConnection(id);
        var result = ModelConnectionConverter.INSTANCE.toVO(connection);
        return ResultModel.success(result);
    }

    /**
     * 接入当前选择的具体模型，不发起外部探测或模型调用。
     * @param dto 接入配置，必须选择平台已登记模型
     * @return 新连接标识
     * @throws ApiRuntimeException 模型不可接入或配置不合法
     */
    @AutoLog("接入模型广场模型")
    @PostMapping("/connections")
    public ResultModel<Long> saveConnection(@Valid @RequestBody ModelConnectionSaveDTO dto) {
        var connection = ModelConnectionConverter.INSTANCE.toEntity(dto);
        var id = biz.createConnection(connection, dto.getApiKey());
        return ResultModel.success(id);
    }

    /**
     * 按版本修改连接配置，不允许切换模型；密钥留空保留原凭证。
     * @param id 连接标识
     * @param dto 完整接入配置及当前版本
     * @return 修改成功标识
     * @throws ApiRuntimeException 模型不可接入、连接不存在或版本冲突
     */
    @AutoLog("修改模型广场连接配置")
    @PutMapping("/connections/{id}")
    public ResultModel<Boolean> updateConnection(@PathVariable @Positive Long id,
            @Validated(UpdateRequest.class) @RequestBody ModelConnectionSaveDTO dto) {
        var connection = ModelConnectionConverter.INSTANCE.toEntity(dto);
        connection.setId(id);
        biz.modifyConnection(connection, dto.getApiKey(), dto.getVersion());
        return ResultModel.success(true);
    }

    /**
     * 独立启停连接，不覆盖已有地址和凭证。
     * @param id 连接标识
     * @param dto 目标启停状态及当前版本
     * @return 修改成功标识
     * @throws ApiRuntimeException 版本冲突或试图启用停用模型的连接
     */
    @AutoLog("修改模型广场连接启停状态")
    @PutMapping("/connections/{id}/status")
    public ResultModel<Boolean> updateConnectionStatus(@PathVariable @Positive Long id,
            @Valid @RequestBody ModelConnectionStatusDTO dto) {
        biz.modifyConnectionStatus(id, dto.getDataStatus(), dto.getVersion());
        return ResultModel.success(true);
    }

    /**
     * 删除的模型连接器。
     * @param id 连接标识
     * @return 删除成功标识
     * @throws ApiRuntimeException 连接不存在或无访问权限
     */
    @AutoLog("删除模型广场连接")
    @DeleteMapping("/connections/{id}")
    public ResultModel<Boolean> removeConnection(@PathVariable @Positive Long id) {
        biz.removeConnection(id);
        return ResultModel.success(true);
    }
}
