package org.etd.framework.starter.ai.memory;

import java.util.List;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.starter.ai.memory.semantic.AgentSearchQueryRewriter;
import org.etd.framework.starter.ai.runtime.AgentRunControl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

/** 官方压缩器的中文输入、上下文和 Token 硬预算回归。 */
class AgentSearchQueryRewriterTest {
    @AfterEach
    void clean() {
        RequestContext.clean();
    }

    @Test
    void reusesOfficialTransformerWithAuthorizedHistoryAndContext() {
        RequestContext.setTenantCode(1L);
        ChatModel model = mock(ChatModel.class);
        AssistantMessage answer = new AssistantMessage("完整检索词");
        Generation generation = new Generation(answer);
        ChatResponse response = new ChatResponse(List.of(generation));
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            assertThat(RequestContext.getTenantCode()).isEqualTo(1L);
            Prompt prompt = invocation.getArgument(0);
            assertThat(prompt.getContents()).contains("历史问题", "那这个呢");
            assertThat(prompt.getOptions().getMaxTokens()).isEqualTo(64);
            return response;
        });
        UserMessage historyQuestion = new UserMessage("历史问题");
        List<Message> history = List.of(historyQuestion);
        AgentRunControl control = mock(AgentRunControl.class);
        AgentSearchQueryRewriter rewriter = new AgentSearchQueryRewriter();
        assertThat(rewriter.rewrite("那这个呢", history, model, control, 2000)).isEqualTo("完整检索词");
        assertThat(RequestContext.getTenantCode()).isEqualTo(1L);
    }

    @Test
    void retainsExistingOriginalQueryStrategyWithoutCallingProviderOverBudget() {
        ChatModel model = mock(ChatModel.class);
        UserMessage historyQuestion = new UserMessage("历史问题");
        List<Message> history = List.of(historyQuestion);
        AgentRunControl control = mock(AgentRunControl.class);
        AgentSearchQueryRewriter rewriter = new AgentSearchQueryRewriter();
        assertThat(rewriter.rewrite("当前问题", history, model, control, 1)).isEqualTo("当前问题");
        verify(model, never()).call(any(Prompt.class));
    }
}
