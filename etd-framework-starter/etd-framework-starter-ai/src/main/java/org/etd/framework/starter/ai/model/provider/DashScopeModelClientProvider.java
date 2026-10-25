package org.etd.framework.starter.ai.model.provider;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankModel;
import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankOptions;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeApiSpec;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.model.Model;
import org.etd.framework.starter.ai.model.client.ModelClientTransport;
import org.etd.framework.starter.ai.model.config.ModelConnectionConfiguration;
import org.etd.framework.starter.ai.model.constant.ModelCapability;
import org.etd.framework.starter.ai.model.constant.ModelClientProtocol;
import org.etd.framework.starter.ai.model.spi.ModelClientProvider;
import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/** 复用Spring AI Alibaba原生聊天、向量与重排SDK，不重新实现HTTP协议。 */
public class DashScopeModelClientProvider implements ModelClientProvider {
    /** 相对配置API根地址的聊天路径。 */
    private static final String CHAT_PATH = "/services/aigc/text-generation/generation";
    /** 相对配置API根地址的向量路径。 */
    private static final String EMBEDDING_PATH = "/services/embeddings/text-embedding/text-embedding";
    /** 相对配置API根地址的重排路径。 */
    private static final String RERANK_PATH = "/services/rerank/text-rerank/text-rerank";
    private final ModelClientTransport transport;
    private final ObservationRegistry observations;

    /** @param transport 共享HTTP传输 @param observations 框架观测注册器 */
    public DashScopeModelClientProvider(ModelClientTransport transport, ObservationRegistry observations) {
        this.transport = transport;
        this.observations = observations;
    }

    @Override
    public boolean supports(String protocol, ModelCapability capability) {
        return ModelClientProtocol.DASHSCOPE.equals(protocol)
                && (capability == ModelCapability.CHAT || capability == ModelCapability.EMBEDDING
                || capability == ModelCapability.RERANK);
    }

    /** 根地址必须包含/api/v1；协议路径固定，不探测或回退到其他地址。 */
    @Override
    public Model<?, ?> create(ModelConnectionConfiguration configuration) {
        if (!supports(configuration.protocol(), configuration.capability())) {
            throw new IllegalArgumentException("DashScope适配器不支持该模型能力");
        }
        var api = createApi(configuration);
        var retry = transport.retryTemplate();
        Model<?, ?> model = switch (configuration.capability()) {
            case CHAT -> {
                var options = DashScopeChatOptions.builder().model(configuration.model()).build();
                yield DashScopeChatModel.builder().dashScopeApi(api).defaultOptions(options)
                        .retryTemplate(retry).observationRegistry(observations).build();
            }
            case EMBEDDING -> {
                var options = DashScopeEmbeddingOptions.builder().model(configuration.model()).build();
                yield DashScopeEmbeddingModel.builder().dashScopeApi(api).defaultOptions(options)
                        .retryTemplate(retry).observationRegistry(observations).build();
            }
            case RERANK -> {
                var options = DashScopeRerankOptions.builder().model(configuration.model()).build();
                yield DashScopeRerankModel.builder().dashScopeApi(api).defaultOptions(options).retryTemplate(retry).build();
            }
            default -> throw new IllegalArgumentException("DashScope适配器不支持该模型能力");
        };
        return model;
    }

    private DashScopeApi createApi(ModelConnectionConfiguration configuration) {
        String secret = configuration.resolveApiKey();
        ApiKey apiKey = secret == null ? new NoopApiKey() : new SimpleApiKey(secret);
        var headers = new LinkedMultiValueMap<String, String>();
        var rest = transport.restClientBuilder();
        // 百炼现有文本向量模型分别返回text_index或index；仅修正当前SDK向量记录的反序列化。
        // 复制客户端专属Mapper，避免影响其他协议、业务HTTP响应和全局Jackson配置。
        rest.messageConverters(converters -> {
            for (var converter : converters) {
                if (converter instanceof MappingJackson2HttpMessageConverter jackson) {
                    var mapper = jackson.getObjectMapper().copy();
                    mapper.addMixIn(DashScopeApiSpec.Embedding.class, EmbeddingIndexMixin.class);
                    jackson.setObjectMapper(mapper);
                }
            }
        });
        var web = transport.webClientBuilder();
        return new DashScopeApi(configuration.baseUrl().toString(), apiKey, headers, null,
                CHAT_PATH, EMBEDDING_PATH, RERANK_PATH, rest, web, RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
    }

    /**
     * SDK 1.1.2.1只声明text_index，qwen3.7文本向量实际返回index；两者都表示原始输入下标。
     * 修正仅限该SDK响应类型；SDK原生支持这两种服务端字段后可删除此映射。
     */
    private abstract static class EmbeddingIndexMixin {
        /** 序号始终来自服务端，禁止用响应列表位置猜测乱序结果对应的正文。 */
        EmbeddingIndexMixin(@JsonProperty("text_index") @JsonAlias("index") Integer textIndex,
                @JsonProperty("embedding") float[] embedding) { }
    }
}
