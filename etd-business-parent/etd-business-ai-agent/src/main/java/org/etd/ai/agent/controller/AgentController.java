package org.etd.ai.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.etd.framework.starter.client.core.permission.annotation.Permission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.etd.ai.agent.biz.AgentBizService;
import org.etd.ai.agent.controller.dto.AgentSaveDTO;
import org.etd.ai.agent.controller.dto.AgentStatusDTO;
import org.etd.ai.agent.controller.dto.AgentVersionDTO;
import org.etd.ai.agent.controller.vo.AgentConfigurationVO;
import org.etd.ai.agent.controller.vo.AgentVO;
import org.etd.ai.agent.controller.vo.AgentWriteVO;
import org.etd.ai.agent.converter.AgentConverter;
import org.etd.ai.agent.service.AgentService;
import org.etd.common.permission.constant.AgentPermissionCode;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.constants.PermissionAction;
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
import org.etd.ai.agent.entity.AgentEntity;

/** Agent列表和编辑页面入口，所有写入通过Biz编排，公开基本详情不包含运行配置。 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/v1/agents")
@Permission(AgentPermissionCode.AGENT)
public class AgentController {
    private final AgentService agents;
    private final AgentBizService biz;

    /** @param current 页码 @param size 页大小 @param keyword 名称 @param dataStatus 启停状态 @param published 发布筛选 @return 可见卡片分页 */
    @GetMapping
    @AutoLog("分页查询Agent列表")
    public ResultModel<IPage<AgentVO>> page(
            @RequestParam(defaultValue = "1") @Min(1) long current,
            @RequestParam(defaultValue = "12") @Min(1) @Max(200) long size,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) @Min(BasicConstant.DataStatus.DISABLED_CODE)
            @Max(BasicConstant.DataStatus.ENABLED_CODE) Integer dataStatus,
            @RequestParam(required = false) Boolean published) {
        IPage<AgentEntity> page = agents.fetchAgentPage(current, size, keyword, dataStatus, published);
        IPage<AgentVO> result = page.convert(AgentConverter.INSTANCE::toVO);
        return ResultModel.success(result);
    }

    /** @param id Agent ID @return 不含系统提示词的基本详情 @throws ApiRuntimeException 不可见 */
    @GetMapping("/{id}")
    @AutoLog("查询Agent基本详情")
    public ResultModel<AgentVO> get(@PathVariable @Positive Long id) {
        AgentEntity agent = agents.requireAgent(id, false, false);
        AgentVO result = AgentConverter.INSTANCE.toVO(agent);
        return ResultModel.success(result);
    }

    /** @param id Agent ID @return 草稿和发布配置，必须同时具备菜单写权限及数据维护资格 */
    @GetMapping("/{id}/configuration")
    @Permission(action = PermissionAction.WRITE)
    @AutoLog("查询Agent编辑配置")
    public ResultModel<AgentConfigurationVO> getConfiguration(@PathVariable @Positive Long id) {
        AgentEntity agent = agents.requireAgent(id, true, false);
        AgentConfigurationVO result = AgentConverter.INSTANCE.toConfigurationVO(agent);
        return ResultModel.success(result);
    }

    /** @param dto 基本信息及可不完整的草稿 @return 新Agent ID及初始版本 */
    @PostMapping
    @AutoLog("创建Agent")
    public ResultModel<AgentWriteVO> save(@Valid @RequestBody AgentSaveDTO dto) {
        AgentEntity input = AgentConverter.INSTANCE.toEntity(dto);
        AgentEntity agent = biz.createAgent(input);
        AgentWriteVO result = AgentConverter.INSTANCE.toWriteVO(agent);
        return ResultModel.success(result);
    }

    /** @param id Agent ID @param dto 完整替换的基本信息和草稿，version必填 @return 最新版本 */
    @PutMapping("/{id}")
    @AutoLog("修改Agent草稿")
    public ResultModel<AgentWriteVO> update(@PathVariable @Positive Long id,
            @Validated(UpdateRequest.class) @RequestBody AgentSaveDTO dto) {
        AgentEntity input = AgentConverter.INSTANCE.toEntity(dto);
        AgentEntity agent = biz.modifyAgent(id, input, dto.getVersion());
        AgentWriteVO result = AgentConverter.INSTANCE.toWriteVO(agent);
        return ResultModel.success(result);
    }

    /** @param id Agent ID @param dto 页面版本 @return 发布后的最新版本 */
    @PostMapping("/{id}/publish")
    @AutoLog("发布Agent")
    public ResultModel<AgentWriteVO> publish(@PathVariable @Positive Long id,
            @Valid @RequestBody AgentVersionDTO dto) {
        AgentEntity agent = biz.publishAgent(id, dto.getVersion());
        AgentWriteVO result = AgentConverter.INSTANCE.toWriteVO(agent);
        return ResultModel.success(result);
    }

    /** @param id Agent ID @param dto 目标状态及版本 @return 最新版本 */
    @PutMapping("/{id}/status")
    @AutoLog("修改Agent启停状态")
    public ResultModel<AgentWriteVO> updateStatus(@PathVariable @Positive Long id,
            @Valid @RequestBody AgentStatusDTO dto) {
        AgentEntity agent = biz.modifyAgentStatus(id, dto.getDataStatus(), dto.getVersion());
        AgentWriteVO result = AgentConverter.INSTANCE.toWriteVO(agent);
        return ResultModel.success(result);
    }

    /** @param id Agent ID @param version 页面当前版本，必填 @return 逻辑删除结果，不自动物理删除聊天历史 */
    @DeleteMapping("/{id}")
    @AutoLog("删除Agent")
    public ResultModel<Boolean> remove(@PathVariable @Positive Long id,
            @RequestParam @Min(0) Long version) {
        biz.removeAgent(id, version);
        return ResultModel.success(true);
    }
}
