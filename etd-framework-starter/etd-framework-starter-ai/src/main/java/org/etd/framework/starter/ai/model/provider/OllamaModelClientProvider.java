package org.etd.framework.starter.ai.model.provider;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.model.Model;
import org.etd.framework.starter.ai.model.client.ModelClientTransport;
import org.etd.framework.starter.ai.model.config.ModelConnectionConfiguration;
import org.etd.framework.starter.ai.model.constant.ModelCapability;
import org.etd.framework.starter.ai.model.constant.ModelClientProtocol;
import org.etd.framework.starter.ai.model.spi.ModelClientProvider;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.ollama.management.PullModelStrategy;
import org.springframework.util.StringUtils;

/** Ollama原生SDK适配；禁止初始化时拉取模型，服务端须已部署所选模型。 */
public class OllamaModelClientProvider implements ModelClientProvider {
    private final ModelClientTransport transport;
    private final ObservationRegistry observations;

    /** @param transport 共享HTTP传输 @param observations 框架观测注册器 */
    public OllamaModelClientProvider(ModelClientTransport transport, ObservationRegistry observations) {
        this.transport = transport;
        this.observations = observations;
    }

    @Override
    public boolean supports(String protocol, ModelCapability capability) {
        return ModelClientProtocol.OLLAMA.equals(protocol)
                && (capability == ModelCapability.CHAT || capability == ModelCapability.EMBEDDING);
    }

    /** 无认证时不发Authorization，配置凭证时仅用于本连接对应的同步和流式客户端。 */
    @Override
    public Model<?, ?> create(ModelConnectionConfiguration configuration) {
        if (!supports(configuration.protocol(), configuration.capability())) {
            throw new IllegalArgumentException("Ollama适配器不支持该模型能力");
        }
        String secret = configuration.resolveApiKey();
        var rest = transport.restClientBuilder();
        var web = transport.webClientBuilder();
        if (StringUtils.hasText(secret)) {
            rest.defaultHeaders(headers -> headers.setBearerAuth(secret));
            web.defaultHeaders(headers -> headers.setBearerAuth(secret));
        }
        var api = OllamaApi.builder().baseUrl(configuration.baseUrl().toString())
                .restClientBuilder(rest).webClientBuilder(web).build();
        var management = ModelManagementOptions.builder().pullModelStrategy(PullModelStrategy.NEVER).build();
        Model<?, ?> model;
        if (configuration.capability() == ModelCapability.CHAT) {
            var options = OllamaChatOptions.builder().model(configuration.model()).build();
            var retry = transport.retryTemplate();
            model = OllamaChatModel.builder().ollamaApi(api).defaultOptions(options).modelManagementOptions(management)
                    .retryTemplate(retry).observationRegistry(observations).build();
        } else {
            var options = OllamaEmbeddingOptions.builder().model(configuration.model()).truncate(false).build();
            model = OllamaEmbeddingModel.builder().ollamaApi(api).defaultOptions(options)
                    .modelManagementOptions(management).observationRegistry(observations).build();
        }
        return model;
    }
}
