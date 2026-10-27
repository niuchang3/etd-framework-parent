package org.etd.ai.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.etd.framework.starter.client.core.permission.annotation.Permission;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.etd.ai.agent.controller.vo.AgentKnowledgeOptionVO;
import org.etd.ai.agent.controller.vo.AgentModelOptionVO;
import org.etd.ai.agent.converter.AgentConverter;
import org.etd.ai.knowledge.service.KnowledgeBaseService;
import org.etd.ai.model.service.ModelConnectionService;
import org.etd.common.permission.constant.AgentPermissionCode;
import org.etd.framework.common.core.constants.PermissionAction;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Agent编辑页面的模型与知识库选择入口，无需额外授予模型管理或知识库维护菜单。 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/v1/agents/options")
@Permission(value = AgentPermissionCode.AGENT, action = PermissionAction.WRITE)
public class AgentOptionController {
    private final ModelConnectionService connections;
    private final KnowledgeBaseService knowledgeBases;

    /** @param current 页码 @param size 页大小 @param keyword 模型名 @return 访问范围内启用的聊天连接，不包含地址及凭证 */
    @GetMapping("/models")
    @AutoLog("查询Agent可选聊天模型")
    public ResultModel<IPage<AgentModelOptionVO>> pageModels(
            @RequestParam(defaultValue = "1") @Min(1) long current,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long size,
            @RequestParam(required = false) @Size(max = 100) String keyword) {
        var page = connections.fetchChatConnectionPage(current, size, keyword);
        var result = page.convert(AgentConverter.INSTANCE::toModelOptionVO);
        return ResultModel.success(result);
    }

    /** @param current 页码 @param size 页大小 @param keyword 知识库名称 @return 访问范围内启用的知识库 */
    @GetMapping("/knowledge-bases")
    @AutoLog("查询Agent可选知识库")
    public ResultModel<IPage<AgentKnowledgeOptionVO>> pageKnowledgeBases(
            @RequestParam(defaultValue = "1") @Min(1) long current,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long size,
            @RequestParam(required = false) @Size(max = 100) String keyword) {
        var page = knowledgeBases.fetchAvailableBasePage(current, size, keyword);
        var result = page.convert(AgentConverter.INSTANCE::toKnowledgeOptionVO);
        return ResultModel.success(result);
    }
}
