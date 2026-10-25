package org.etd.framework.starter.ai.model.provider;

import io.micrometer.observation.ObservationRegistry;
import org.etd.framework.starter.ai.model.client.ModelClientTransport;
import org.etd.framework.starter.ai.model.config.ModelConnectionConfiguration;
import org.etd.framework.starter.ai.model.constant.ModelCapability;
import org.etd.framework.starter.ai.model.constant.ModelClientProtocol;
import org.etd.framework.starter.ai.model.spi.ModelClientProvider;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.Model;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.moderation.ModerationPrompt;
import org.springframework.ai.moderation.ModerationResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.openai.OpenAiModerationModel;
import org.springframework.ai.openai.OpenAiModerationOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.ai.openai.api.OpenAiModerationApi;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;
import org.springframework.retry.support.RetryTemplate;

/**
 * 使用OpenAI原生SDK创建聊天、向量、图像、音频和审核客户端，不猜测独立重排协议。
 * 兼容服务是否实现具体端点由服务自身决定；创建客户端不会探测远端能力。
 */
public class OpenAiModelClientProvider implements ModelClientProvider {
    /** 相对配置API根地址的聊天路径，不再次追加/v1。 */
    private static final String CHAT_PATH = "/chat/completions";
    /** 相对配置API根地址的向量路径。 */
    private static final String EMBEDDING_PATH = "/embeddings";
    /** 相对配置API根地址的图片生成路径。 */
    private static final String IMAGE_PATH = "/images/generations";
    private final ModelClientTransport transport;
    private final ObservationRegistry observations;

    /** @param transport 共享HTTP传输 @param observations 框架观测注册器 */
    public OpenAiModelClientProvider(ModelClientTransport transport, ObservationRegistry observations) {
        this.transport = transport;
        this.observations = observations;
    }

    @Override
    public boolean supports(String protocol, ModelCapability capability) {
        if (!ModelClientProtocol.OPENAI_COMPATIBLE.equals(protocol) || capability == null) {
            return false;
        }
        return switch (capability) {
            case CHAT, EMBEDDING, IMAGE, SPEECH_TO_TEXT, TEXT_TO_SPEECH, MODERATION -> true;
            case RERANK -> false;
        };
    }

    /** 创建客户端不发起网络探测；调用重试由上层业务显式决定。 */
    @Override
    public Model<?, ?> create(ModelConnectionConfiguration configuration) {
        if (!supports(configuration.protocol(), configuration.capability())) {
            throw new IllegalArgumentException("OpenAI适配器不支持该模型能力");
        }
        String secret = configuration.resolveApiKey();
        ApiKey apiKey = secret == null ? new NoopApiKey() : new SimpleApiKey(secret);
        return switch (configuration.capability()) {
            case CHAT -> createChat(configuration, apiKey);
            case EMBEDDING -> createEmbedding(configuration, apiKey);
            case IMAGE -> createImage(configuration, apiKey);
            case SPEECH_TO_TEXT -> createTranscription(configuration, apiKey);
            case TEXT_TO_SPEECH -> createSpeech(configuration, apiKey);
            case MODERATION -> createModeration(configuration, apiKey);
            case RERANK -> throw new IllegalArgumentException("OpenAI协议不提供独立重排接口");
        };
    }

    private OpenAiApi createApi(ModelConnectionConfiguration configuration, ApiKey apiKey) {
        var rest = transport.restClientBuilder();
        var web = transport.webClientBuilder();
        return OpenAiApi.builder().baseUrl(configuration.baseUrl().toString()).apiKey(apiKey)
                .completionsPath(CHAT_PATH).embeddingsPath(EMBEDDING_PATH)
                .restClientBuilder(rest).webClientBuilder(web).build();
    }

    private OpenAiChatModel createChat(ModelConnectionConfiguration configuration, ApiKey apiKey) {
        var api = createApi(configuration, apiKey);
        var retry = transport.retryTemplate();
        var options = OpenAiChatOptions.builder().model(configuration.model()).build();
        return OpenAiChatModel.builder().openAiApi(api).defaultOptions(options)
                .retryTemplate(retry).observationRegistry(observations).build();
    }

    private OpenAiEmbeddingModel createEmbedding(ModelConnectionConfiguration configuration, ApiKey apiKey) {
        var api = createApi(configuration, apiKey);
        var retry = transport.retryTemplate();
        var options = OpenAiEmbeddingOptions.builder().model(configuration.model()).build();
        return new OpenAiEmbeddingModel(api, MetadataMode.EMBED, options, retry, observations);
    }

    private OpenAiImageModel createImage(ModelConnectionConfiguration configuration, ApiKey apiKey) {
        var rest = transport.restClientBuilder();
        var api = OpenAiImageApi.builder().baseUrl(configuration.baseUrl().toString()).apiKey(apiKey)
                .imagesPath(IMAGE_PATH).restClientBuilder(rest).build();
        var options = OpenAiImageOptions.builder().model(configuration.model()).build();
        var retry = transport.retryTemplate();
        return new OpenAiImageModel(api, options, retry, observations);
    }

    /** 沿用SDK的alloy音色与MP3格式；单次请求可覆盖，不保存单次合成内容。 */
    private OpenAiAudioSpeechModel createSpeech(ModelConnectionConfiguration configuration, ApiKey apiKey) {
        var api = createAudioApi(configuration, apiKey);
        var options = OpenAiAudioSpeechOptions.builder().model(configuration.model())
                .voice(OpenAiAudioApi.SpeechRequest.Voice.ALLOY)
                .responseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3).build();
        var retry = transport.retryTemplate();
        return new OpenAiAudioSpeechModel(api, options, retry);
    }

    private OpenAiAudioTranscriptionModel createTranscription(ModelConnectionConfiguration configuration, ApiKey apiKey) {
        var api = createAudioApi(configuration, apiKey);
        // SDK根据响应格式选择反序列化类型，默认显式使用JSON，避免空格式导致调用失败。
        var options = OpenAiAudioTranscriptionOptions.builder().model(configuration.model())
                .responseFormat(OpenAiAudioApi.TranscriptResponseFormat.JSON).build();
        var retry = transport.retryTemplate();
        return new OpenAiAudioTranscriptionModel(api, options, retry);
    }

    private OpenAiAudioApi createAudioApi(ModelConnectionConfiguration configuration, ApiKey apiKey) {
        var uris = new ApiRootUriBuilderFactory(configuration.baseUrl().toString());
        var rest = transport.restClientBuilder().uriBuilderFactory(uris);
        var web = transport.webClientBuilder().uriBuilderFactory(uris);
        return OpenAiAudioApi.builder().baseUrl(configuration.baseUrl().toString()).apiKey(apiKey)
                .restClientBuilder(rest).webClientBuilder(web).build();
    }

    private OpenAiModerationModel createModeration(ModelConnectionConfiguration configuration, ApiKey apiKey) {
        var uris = new ApiRootUriBuilderFactory(configuration.baseUrl().toString());
        var rest = transport.restClientBuilder().uriBuilderFactory(uris);
        var api = OpenAiModerationApi.builder().baseUrl(configuration.baseUrl().toString()).apiKey(apiKey)
                .restClientBuilder(rest).build();
        var retry = transport.retryTemplate();
        var options = OpenAiModerationOptions.builder().model(configuration.model()).build();
        var model = new ConnectionModerationModel(api, retry);
        // 仅在尚未发布到缓存前设置默认项，业务调用期间不得修改共享模型配置。
        return model.withDefaultOptions(options);
    }

    /** 修正当前SDK将空请求选项转换为供应商默认模型的问题，不修改调用方Prompt或共享默认项。 */
    private static final class ConnectionModerationModel extends OpenAiModerationModel {
        private ConnectionModerationModel(OpenAiModerationApi api, RetryTemplate retry) {
            super(api, retry);
        }

        /** 未指定请求模型时省略请求选项，由原生SDK使用已配置的连接默认模型。 */
        @Override
        public ModerationResponse call(ModerationPrompt prompt) {
            if (prompt.getOptions() == null || prompt.getOptions().getModel() == null) {
                var defaultPrompt = new ModerationPrompt(prompt.getInstructions(), null);
                return super.call(defaultPrompt);
            }
            return super.call(prompt);
        }
    }

    /**
     * Spring AI 1.1.2音频与审核API固定写入v1路径且未提供路径配置项。
     * 只移除SDK自带的版本段，再由Spring拼接用户配置的完整API根路径，保留网关前缀。
     */
    private static final class ApiRootUriBuilderFactory extends DefaultUriBuilderFactory {
        /** SDK写入的版本段，不代表业务连接必须使用该前缀。 */
        private static final String SDK_VERSION_PREFIX = "v1/";

        private ApiRootUriBuilderFactory(String apiRoot) {
            super(apiRoot);
        }

        /** @param uriTemplate SDK的固定相对路径 @return 相对已配置API根地址的构造器 */
        @Override
        public UriBuilder uriString(String uriTemplate) {
            String relative = uriTemplate.startsWith("/") ? uriTemplate.substring(1) : uriTemplate;
            if (!relative.startsWith(SDK_VERSION_PREFIX)) {
                throw new IllegalArgumentException("非预期的OpenAI SDK端点路径");
            }
            return super.uriString("/" + relative.substring(SDK_VERSION_PREFIX.length()));
        }
    }
}
