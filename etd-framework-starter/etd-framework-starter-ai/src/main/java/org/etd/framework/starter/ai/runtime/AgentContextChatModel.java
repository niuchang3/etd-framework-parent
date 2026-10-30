package org.etd.framework.starter.ai.runtime;

import java.util.Map;
import java.util.function.Consumer;
import org.etd.framework.starter.ai.model.client.ModelRequestHeaders;
import org.etd.framework.starter.ai.runtime.context.RequestContextSnapshot;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;

/** 官方图执行与模型 SDK 之间的上下文边界；仅适配身份和请求头，不实现模型调用协议。 */
final class AgentContextChatModel implements ChatModel {
    private final ChatModel model;
    private final RequestContextSnapshot snapshot;
    private final Context headers;
    /** 在官方图过滤无正文的用量事件前记录统计，避免丢失流末尾 Token 信息。 */
    private final Consumer<ChatResponse> responseObserver;

    AgentContextChatModel(ChatModel model, RequestContextSnapshot snapshot, Consumer<ChatResponse> responseObserver) {
        this.model = model;
        this.responseObserver = responseObserver;
        this.snapshot = snapshot;
        Map<String, String> values = ModelRequestHeaders.capture();
        this.headers = Context.of(ModelRequestHeaders.REACTOR_KEY, values);
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return model.getDefaultOptions();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        return snapshot.call(() -> model.call(prompt));
    }

    /** 延迟创建 SDK 流，使同步异常成为流错误，避免官方节点将异常文本当作成功回答。 */
    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return Flux.defer(() -> openModelStream(prompt)).contextWrite(headers);
    }

    private Flux<ChatResponse> openModelStream(Prompt prompt) {
        Flux<ChatResponse> responses = snapshot.call(() -> model.stream(prompt));
        return responses.doOnNext(responseObserver);
    }
}
