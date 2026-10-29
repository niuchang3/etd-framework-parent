package org.etd.ai.agent;

import java.util.List;
import org.etd.ai.agent.biz.AgentChatBizService;
import org.etd.ai.agent.config.AgentChatProperties;
import org.etd.ai.agent.constant.AgentChatPolicy;
import org.etd.ai.agent.entity.AgentConversationEntity;
import org.etd.ai.agent.entity.AgentEntity;
import org.etd.ai.agent.entity.AgentRunEntity;
import org.etd.ai.agent.model.AgentConfiguration;
import org.etd.ai.agent.service.AgentConversationService;
import org.etd.ai.agent.service.AgentMessageService;
import org.etd.ai.agent.service.AgentRunService;
import org.etd.ai.agent.service.AgentService;
import org.etd.ai.knowledge.biz.KnowledgeRecallBizService;
import org.etd.ai.knowledge.service.KnowledgeModelSettingsService;
import org.etd.ai.model.entity.ModelConnectionEntity;
import org.etd.ai.model.service.ModelConnectionService;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.starter.ai.model.client.ModelClientRegistry;
import org.etd.framework.starter.ai.runtime.config.AgentRuntimeProperties;
import org.etd.framework.starter.ai.runtime.model.AgentRunRequest;
import org.etd.framework.starter.ai.runtime.spi.AgentRunEventListener;
import org.etd.framework.starter.ai.runtime.spi.AgentRunRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 从公开的运行准备与检索插槽验证追问重写，不通过反射测试私有辅助方法。 */
class AgentSearchRewriteTest {
    /** 真实聊天编排入口。 */
    private AgentChatBizService biz;
    /** 可控的历史读取能力。 */
    private AgentMessageService messages;
    /** 校验实际使用的检索词。 */
    private KnowledgeRecallBizService knowledge;
    /** 不连接外部模型，提供确定性重写结果。 */
    private ChatModel model;

    /** 只准备必要业务对象，真正的模型和知识库调用发生在检索插槽被执行时。 */
    @BeforeEach
    void initialize() {
        AgentService agents = mock(AgentService.class);
        AgentConversationService conversations = mock(AgentConversationService.class);
        messages = mock(AgentMessageService.class);
        AgentRunService runs = mock(AgentRunService.class);
        ModelConnectionService connections = mock(ModelConnectionService.class);
        KnowledgeModelSettingsService settings = mock(KnowledgeModelSettingsService.class);
        AgentChatProperties properties = new AgentChatProperties();
        AgentRunRecordService records = mock(AgentRunRecordService.class);
        AgentRuntimeProperties runtime = new AgentRuntimeProperties();
        ModelClientRegistry clients = mock(ModelClientRegistry.class);
        knowledge = mock(KnowledgeRecallBizService.class);
        model = mock(ChatModel.class);
        biz = new AgentChatBizService(agents, conversations, messages, runs, connections, settings,
                properties, records, runtime, clients, knowledge);
        prepareBusinessRecords(agents, conversations, connections, runs);
        when(clients.getChatModel("1", "100")).thenReturn(model);
        when(knowledge.recallEnabledList(anyList(), anyString())).thenReturn(List.of());
    }

    /** 模型只看最近四条历史，输出去掉首尾空白后用于召回，当前回答问题保持原样。 */
    @Test
    void rewritesQueryUsingRecentHistory() {
        List<Message> history = history();
        when(messages.fetchHistoryList(eq(1L), anyInt())).thenReturn(history);
        ChatResponse rewritten = response("  打印机Wi-Fi配置步骤  ");
        when(model.call(any(Prompt.class))).thenReturn(rewritten);
        AgentRunRequest request = queryKnowledge();
        ArgumentCaptor<Prompt> captured = ArgumentCaptor.forClass(Prompt.class);
        verify(model).call(captured.capture());
        Prompt prompt = captured.getValue();
        assertThat(prompt.getInstructions()).hasSize(6);
        assertThat(prompt.getContents()).contains("最近问题", "最近答案", "最新追问：它怎么配置").doesNotContain("最早问题", "最早答案");
        assertThat(prompt.getOptions().getMaxTokens()).isEqualTo(64);
        assertThat(prompt.getOptions().getTemperature()).isEqualTo(0.1);
        List<Long> ids = List.of(10L);
        verify(knowledge).recallEnabledList(ids, "打印机Wi-Fi配置步骤");
        assertThat(request.chatRequest().question()).isEqualTo("它怎么配置");
    }

    /** 没有历史时不发起额外模型请求，直接使用当前问题召回。 */
    @Test
    void skipsRewriteWithoutHistory() {
        when(messages.fetchHistoryList(eq(1L), anyInt())).thenReturn(List.of());
        queryKnowledge();
        verifyNoInteractions(model);
        verifyOriginalQuery();
    }

    /** 模型无响应时仍沿用已有的原问题处理策略。 */
    @Test
    void retainsOriginalQueryWhenModelReturnsNoResponse() {
        when(messages.fetchHistoryList(eq(1L), anyInt())).thenReturn(history());
        queryKnowledge();
        verifyOriginalQuery();
    }

    /** 重写失败不改变既有召回流程，最终回答问题仍为用户输入。 */
    @Test
    void retainsOriginalQueryWhenRewriteFails() {
        when(messages.fetchHistoryList(eq(1L), anyInt())).thenReturn(history());
        IllegalStateException failure = new IllegalStateException("synthetic-model-failure");
        when(model.call(any(Prompt.class))).thenThrow(failure);
        queryKnowledge();
        verifyOriginalQuery();
    }

    /** 通过公开运行定义触发真实检索回调，避免绕过模型延迟创建边界。 */
    private AgentRunRequest queryKnowledge() {
        AgentRunEventListener listener = mock(AgentRunEventListener.class);
        AgentRunRequest request = biz.prepareRun(1L, "rewrite-test", "它怎么配置", listener);
        request.chatRequest().knowledgeService().queryKnowledgeList(request.chatRequest().question());
        return request;
    }

    /** 统一验证已有的原问题召回策略。 */
    private void verifyOriginalQuery() {
        List<Long> ids = List.of(10L);
        verify(knowledge).recallEnabledList(ids, "它怎么配置");
    }

    /** 模拟会话、发布配置和本轮运行，不依赖数据库或收费服务。 */
    private void prepareBusinessRecords(AgentService agents, AgentConversationService conversations,
            ModelConnectionService connections, AgentRunService runs) {
        AgentConversationEntity session = new AgentConversationEntity();
        session.setId(1L);
        session.setAgentId(9L);
        session.setConversationType(AgentChatPolicy.CHAT);
        when(conversations.requireConversation(1L, true)).thenReturn(session);
        AgentConfiguration configuration = new AgentConfiguration();
        configuration.setModelConnectionId(100L);
        configuration.setSystemPrompt("回答用户问题");
        List<Long> ids = List.of(10L);
        configuration.setKnowledgeBaseIds(ids);
        AgentEntity agent = new AgentEntity();
        agent.setDataStatus(BasicConstant.DataStatus.ENABLED_CODE);
        agent.setPublishedConfig(configuration);
        when(agents.requireAgent(9L, false, false)).thenReturn(agent);
        ModelConnectionEntity connection = new ModelConnectionEntity();
        connection.setTenantId(1L);
        when(connections.requireConnection(100L, false)).thenReturn(connection);
        AgentRunEntity run = new AgentRunEntity();
        run.setId(3L);
        run.setConversationId(1L);
        when(runs.createRun(eq(session), anyString(), eq(100L), anyInt())).thenReturn(run);
        when(messages.createMessagePair(eq(session), eq(3L), anyString())).thenReturn(2L);
    }

    /** 三轮问答用于验证重写只读取最近两轮。 */
    private List<Message> history() {
        UserMessage oldQuestion = new UserMessage("最早问题");
        AssistantMessage oldAnswer = new AssistantMessage("最早答案");
        UserMessage question = new UserMessage("打印机支持什么连接方式");
        AssistantMessage answer = new AssistantMessage("支持Wi-Fi连接");
        UserMessage recentQuestion = new UserMessage("最近问题");
        AssistantMessage recentAnswer = new AssistantMessage("最近答案");
        return List.of(oldQuestion, oldAnswer, question, answer, recentQuestion, recentAnswer);
    }

    /** 创建完整模型响应，覆盖正常文本读取路径。 */
    private ChatResponse response(String text) {
        AssistantMessage message = new AssistantMessage(text);
        Generation generation = new Generation(message);
        List<Generation> generations = List.of(generation);
        return new ChatResponse(generations);
    }
}
