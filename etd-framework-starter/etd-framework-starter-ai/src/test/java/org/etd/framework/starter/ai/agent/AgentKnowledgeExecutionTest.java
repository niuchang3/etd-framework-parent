package org.etd.framework.starter.ai.agent;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.etd.framework.starter.ai.memory.AgentMemoryManager;
import org.etd.framework.starter.ai.memory.semantic.AgentKnowledgeMode;
import org.etd.framework.starter.ai.memory.semantic.AgentKnowledgeQueryService;
import org.etd.framework.starter.ai.memory.semantic.AgentKnowledgeTool;
import org.etd.framework.starter.ai.model.client.ModelClientRegistry;
import org.etd.framework.starter.ai.runtime.AgentRunControl;
import org.etd.framework.starter.ai.runtime.config.AgentRuntimeProperties;
import org.etd.framework.starter.ai.runtime.model.AgentChatPreparation;
import org.springframework.ai.document.Document;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 三种知识模式走同一官方图，验证工具按需召回、范围绑定、引用回传及不重复预检索。 */
class AgentKnowledgeExecutionTest {
    @ParameterizedTest
    @EnumSource(AgentKnowledgeMode.class)
    void keepsKnowledgeModesExclusiveInsideOfficialGraph(AgentKnowledgeMode mode) {
        AtomicInteger recalls = new AtomicInteger();
        AtomicInteger rounds = new AtomicInteger();
        Document fragment = new Document("chunk", "授权知识正文", Map.of());
        AgentKnowledgeQueryService knowledge = mock(AgentKnowledgeQueryService.class);
        List<Long> baseIds = List.of(7L);
        when(knowledge.queryKnowledge(any(), any(), any())).thenAnswer(invocation -> {
            Long actualScope = invocation.getArgument(0);
            assertThat(actualScope).isEqualTo(7L);
            recalls.incrementAndGet();
            return List.of(fragment);
        });
        ChatModel model = mock(ChatModel.class);
        when(model.stream(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            int round = rounds.incrementAndGet();
            if (mode == AgentKnowledgeMode.TOOL && round == 1) {
                assertThat(recalls.get()).isZero();
                return Flux.just(decision());
            }
            if (mode == AgentKnowledgeMode.TOOL) {
                assertThat(prompt.getInstructions()).anySatisfy(message -> {
                    assertThat(message).isInstanceOf(ToolResponseMessage.class);
                    ToolResponseMessage response = (ToolResponseMessage) message;
                    assertThat(response.getResponses().getFirst().responseData()).contains("[1]", "授权知识正文");
                });
            }
            if (mode == AgentKnowledgeMode.PRELOAD) {
                assertThat(recalls.get()).isEqualTo(1);
                assertThat(prompt.getInstructions()).anySatisfy(message -> assertThat(message.getText()).contains("授权知识正文"));
            }
            return Flux.just(answer());
        });
        ModelClientRegistry models = mock(ModelClientRegistry.class);
        when(models.getChatModel(any(), any())).thenReturn(model);
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        beans.registerSingleton("models", models);
        beans.registerSingleton("knowledge", knowledge);
        AgentMemoryManager memory = new AgentMemoryManager();
        AgentRuntimeProperties properties = new AgentRuntimeProperties();
        ReactAgentExecutor executor = new ReactAgentExecutor(memory, null, beans.getBeanProvider(ModelClientRegistry.class),
                beans.getBeanProvider(AgentKnowledgeQueryService.class), beans.getBeanProvider(ToolCallbackProvider.class), properties);
        AgentChatPreparation prepared = new AgentChatPreparation("knowledge", "1", "100", "系统规则", List.of(),
                null, null, baseIds, List.of(), true, mode, null, null, null, Map.of());
        AgentRunControl control = mock(AgentRunControl.class);
        List<String> answer = executor.execute(prepared, "问题", control).collectList().block(Duration.ofSeconds(5));
        assertThat(answer).containsExactly("回答[1]");
        assertThat(recalls.get()).isEqualTo(mode == AgentKnowledgeMode.NONE ? 0 : 1);
        assertThat(rounds.get()).isEqualTo(mode == AgentKnowledgeMode.TOOL ? 2 : 1);
        if (mode == AgentKnowledgeMode.TOOL) {
            verify(knowledge, times(2)).requireAuthorized(baseIds);
            verify(control).recordReferences(List.of(fragment));
        }
    }

    private ChatResponse decision() {
        AssistantMessage.ToolCall call = new AssistantMessage.ToolCall("knowledge-1", "function", AgentKnowledgeTool.NAME,
                "{\"query\":\"资料\",\"tenantId\":999,\"baseIds\":[999]}");
        List<AssistantMessage.ToolCall> calls = List.of(call);
        AssistantMessage message = AssistantMessage.builder().content("").toolCalls(calls).build();
        Generation generation = new Generation(message);
        return new ChatResponse(List.of(generation));
    }

    private ChatResponse answer() {
        AssistantMessage message = new AssistantMessage("回答[1]");
        Generation generation = new Generation(message);
        return new ChatResponse(List.of(generation));
    }
}
