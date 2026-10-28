package org.etd.ai.agent.controller;

import com.etd.framework.starter.client.core.permission.annotation.Permission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.etd.ai.agent.biz.AgentChatBizService;
import org.etd.ai.agent.biz.AgentRunBizService;
import org.etd.framework.starter.ai.runtime.AgentRuntime;
import org.etd.framework.starter.ai.runtime.config.AgentRuntimeProperties;
import org.etd.ai.agent.controller.stream.AgentChatSseListener;
import java.util.concurrent.TimeUnit;
import org.etd.ai.agent.controller.dto.AgentChatMessageDTO;
import org.etd.ai.agent.controller.dto.AgentChatSessionDTO;
import org.etd.ai.agent.controller.vo.AgentChatSessionVO;
import org.etd.ai.agent.controller.vo.AgentChatMessageVO;
import org.etd.ai.agent.controller.vo.AgentChatRunVO;
import org.etd.ai.agent.controller.vo.AgentChatProfileVO;
import org.etd.ai.agent.service.AgentService;
import org.etd.ai.agent.converter.AgentChatConverter;
import org.etd.ai.agent.service.AgentConversationService;
import org.etd.ai.agent.service.AgentMessageService;
import org.etd.common.permission.constant.AgentPermissionCode;
import org.etd.framework.common.core.constants.PermissionAction;
import org.etd.framework.common.core.constants.HeaderConstant;
import org.etd.ai.agent.constant.AgentChatPolicy;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.etd.ai.agent.entity.AgentConversationEntity;
import org.etd.ai.agent.entity.AgentEntity;
import org.etd.ai.agent.entity.AgentMessageEntity;
import org.etd.ai.agent.entity.AgentRunEntity;
import org.etd.ai.agent.model.AgentConfiguration;
import org.etd.framework.starter.ai.runtime.AgentRunControl;

/** Agent聊天入口；流式请求复用框架认证和请求头上下文，个人会话不随Agent公开。 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/agent/chat")
@Permission(value = AgentPermissionCode.AGENT, action = PermissionAction.READ)
public class AgentChatController {
    private final AgentChatBizService biz;
    private final AgentRunBizService runBiz;
    private final AgentService agents;
    private final AgentConversationService conversations;
    private final AgentMessageService messages;
    private final AgentRuntime runtime;
    private final AgentRuntimeProperties runtimeProperties;

    /** @param agentId Agent ID @param conversationType 正式或调试 @return 聊天页面开场白与推荐问题，不含内部指令 */
    @GetMapping("/agents/{agentId}")
    @AutoLog("查询Agent聊天展示配置")
    public ResultModel<AgentChatProfileVO> getProfile(@PathVariable @Positive Long agentId,
            @RequestParam(defaultValue = "chat") @Pattern(regexp = AgentChatPolicy.CONVERSATION_TYPE_PATTERN) String conversationType) {
        AgentConfiguration configuration = biz.requireConfiguration(agentId, conversationType);
        AgentEntity agent = agents.requireAgent(agentId, false, false);
        AgentChatProfileVO result = AgentChatConverter.INSTANCE.toProfileVO(agent, configuration);
        return ResultModel.success(result);
    }

    /** @param dto Agent、会话类型与标题 @return 创建的个人会话 */
    @PostMapping("/sessions")
    @AutoLog("创建Agent聊天会话")
    public ResultModel<AgentChatSessionVO> saveSession(@Valid @RequestBody AgentChatSessionDTO dto) {
        AgentConversationEntity session = biz.createSession(dto.getAgentId(), dto.getConversationType(), dto.getTitle());
        AgentChatSessionVO result = AgentChatConverter.INSTANCE.toSessionVO(session);
        return ResultModel.success(result);
    }

    /** @param agentId 可选Agent筛选 @param beforeId 向前游标 @param size 页大小 @return 按ID降序的个人会话列表 */
    @GetMapping("/sessions")
    @AutoLog("查询Agent聊天会话列表")
    public ResultModel<List<AgentChatSessionVO>> listSessions(@RequestParam(required = false) @Positive Long agentId,
            @RequestParam(required = false) @Positive Long beforeId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        List<AgentConversationEntity> rows = conversations.fetchConversationList(agentId, beforeId, size);
        List<AgentChatSessionVO> result = rows.stream().map(AgentChatConverter.INSTANCE::toSessionVO).toList();
        return ResultModel.success(result);
    }

    /** @param sessionId 个人会话 @param beforeSequence 向前消息序号游标 @param size 页大小 @return 本页按序号升序的消息 */
    @GetMapping("/sessions/{sessionId}/messages")
    @AutoLog("查询Agent聊天消息历史")
    public ResultModel<List<AgentChatMessageVO>> listMessages(@PathVariable @Positive Long sessionId,
            @RequestParam(required = false) @Positive Long beforeSequence,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        conversations.requireConversation(sessionId, false);
        List<AgentMessageEntity> rows = messages.fetchMessageList(sessionId, beforeSequence, size);
        List<AgentChatMessageVO> result = rows.stream().map(AgentChatConverter.INSTANCE::toMessageVO).toList();
        return ResultModel.success(result);
    }

    /** @param sessionId 会话 @param dto 幂等ID及问题 @param response 关闭代理缓冲的响应 @return SSE事件流 */
    @PostMapping(value = "/sessions/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @AutoLog("流式生成Agent回答")
    public SseEmitter stream(@PathVariable @Positive Long sessionId,
            @Valid @RequestBody AgentChatMessageDTO dto, HttpServletResponse response) {
        SseEmitter emitter = new SseEmitter(TimeUnit.SECONDS.toMillis(runtimeProperties.getTimeoutSeconds() + 10L));
        AgentChatSseListener output = new AgentChatSseListener(emitter);
        AgentRunControl handle = runtime.start(() -> biz.prepareRun(sessionId, dto.getRequestId(), dto.getContent(), output));
        output.bind(handle);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        response.setHeader(HeaderConstant.X_ACCEL_BUFFERING, "no");
        return emitter;
    }

    /** @param runId 本人会话的运行 @return 当前执行状态 */
    @GetMapping("/runs/{runId}")
    @AutoLog("查询Agent回答执行状态")
    public ResultModel<AgentChatRunVO> getRun(@PathVariable @Positive Long runId) {
        AgentRunEntity run = runBiz.requireOwnedRun(runId);
        AgentChatRunVO result = AgentChatConverter.INSTANCE.toRunVO(run);
        return ResultModel.success(result);
    }

    /** @param sessionId 本人会话 @param requestId 原始提交标识 @return 原运行状态，不重新执行 */
    @GetMapping("/sessions/{sessionId}/runs")
    @AutoLog("按请求标识查询Agent执行状态")
    public ResultModel<AgentChatRunVO> getRunByRequest(@PathVariable @Positive Long sessionId,
            @RequestParam @NotBlank @Size(max = 64) String requestId) {
        AgentRunEntity run = runBiz.requireOwnedRunByRequest(sessionId, requestId);
        AgentChatRunVO result = AgentChatConverter.INSTANCE.toRunVO(run);
        return ResultModel.success(result);
    }

    /** @param runId 本人会话的运行 @return 请求取消后的状态，取消中仍占用会话 */
    @PostMapping("/runs/{runId}/cancel")
    @AutoLog("停止生成Agent回答")
    public ResultModel<AgentChatRunVO> cancel(@PathVariable @Positive Long runId) {
        AgentRunEntity run = runBiz.cancelRun(runId);
        AgentChatRunVO result = AgentChatConverter.INSTANCE.toRunVO(run);
        return ResultModel.success(result);
    }
}
