package org.etd.ai.model;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.etd.ai.model.component.BusinessModelConnectionResolver;
import org.etd.ai.model.component.ModelCredentialCipher;
import org.etd.ai.model.config.ModelCredentialProperties;
import org.etd.ai.model.constant.ModelProtocol;
import org.etd.ai.model.constant.ModelType;
import org.etd.ai.model.entity.ModelConnectionEntity;
import org.etd.ai.model.entity.ModelEntity;
import org.etd.ai.model.mapper.ModelConnectionMapper;
import org.etd.ai.model.mapper.ModelMapper;
import org.etd.ai.model.service.ModelInferenceService;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.ai.model.autoconfigure.ModelClientAutoConfiguration;
import org.etd.framework.starter.ai.model.client.ModelClientRegistry;
import org.etd.framework.starter.ai.model.constant.ModelClientProtocol;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 贯通业务密文、真实解密、框架自动装配、原生SDK及HTTP认证头，不使用真实模型凭证。 */
class ModelCredentialInvocationTest {
    private HttpServer server;
    private ModelConnectionMapper connections;
    private ModelMapper models;
    private ModelConnectionEntity connection;
    private ModelCredentialCipher cipher;
    private String apiKey;
    private final List<String> authorizationHeaders = new CopyOnWriteArrayList<>();

    /** 每次生成临时密钥及凭证，只在本地随机端口接收SDK请求。 */
    @BeforeEach
    void prepare() throws IOException {
        var address = new InetSocketAddress("127.0.0.1", 0);
        server = HttpServer.create(address, 0);
        server.createContext("/v1/embeddings", this::respond);
        server.start();
        cipher = createCipher();
        apiKey = UUID.randomUUID().toString();
        connection = createConnection();
        encryptCredential(apiKey);
        connections = mock(ModelConnectionMapper.class);
        models = mock(ModelMapper.class);
        when(connections.selectById(30L)).thenReturn(connection);
        when(connections.selectById("30")).thenReturn(connection);
        var model = createModel();
        when(models.selectById(20L)).thenReturn(model);
    }

    /** 关闭监听端口；Spring上下文会关闭框架Registry和共享HTTP传输。 */
    @AfterEach
    void closeServer() {
        server.stop(0);
    }

    /** 原生SDK必须发送业务解密后的Bearer值；更新连接版本后必须使用新的凭证。 */
    @Test
    void passesDecryptedCredentialThroughFrameworkToSdk() {
        var runner = createContextRunner();
        runner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(ModelClientRegistry.class);
            var registry = context.getBean(ModelClientRegistry.class);
            var inference = new ModelInferenceService(connections, registry);
            var texts = List.of("测试正文");
            assertThat(inference.createEmbeddingList(30L, 2, texts)).hasSize(1);
            assertThat(authorizationHeaders).containsExactly("Bearer " + apiKey);
            assertThat(connection.getApiKeyCiphertext()).doesNotContain(apiKey);
            String replacement = UUID.randomUUID().toString();
            encryptCredential(replacement);
            connection.setVersion(1L);
            assertThat(inference.createEmbeddingList(30L, 2, texts)).hasSize(1);
            assertThat(authorizationHeaders).containsExactly("Bearer " + apiKey, "Bearer " + replacement);
        });
    }

    /** 密文损坏时必须在SDK请求发出前失败，不能悄悄降级成无认证调用。 */
    @Test
    void rejectsInvalidCiphertextBeforeSendingRequest() {
        connection.setApiKeyCiphertext(UUID.randomUUID().toString());
        var runner = createContextRunner();
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            var registry = context.getBean(ModelClientRegistry.class);
            var inference = new ModelInferenceService(connections, registry);
            var texts = List.of("测试正文");
            assertThatThrownBy(() -> inference.createEmbeddingList(30L, 2, texts))
                    .isInstanceOf(ApiRuntimeException.class).hasMessageContaining("向量生成失败");
            assertThat(authorizationHeaders).isEmpty();
        });
    }

    /** 真实装配业务Resolver及框架自动配置，避免手动new Registry绕过Bean注册问题。 */
    private ApplicationContextRunner createContextRunner() {
        var resolver = new BusinessModelConnectionResolver(connections, models, cipher);
        var configuration = AutoConfigurations.of(ModelClientAutoConfiguration.class);
        return new ApplicationContextRunner().withConfiguration(configuration)
                .withBean(BusinessModelConnectionResolver.class, () -> resolver);
    }

    /** 测试主密钥随机生成，不向工程写入固定密钥或服务商访问令牌。 */
    private ModelCredentialCipher createCipher() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        var properties = new ModelCredentialProperties();
        properties.setKey(Base64.getEncoder().encodeToString(key));
        properties.setKeyId(UUID.randomUUID().toString());
        return new ModelCredentialCipher(properties);
    }

    /** 模拟持久化后的连接，SDK根地址只指向本测试的回环HTTP服务。 */
    private ModelConnectionEntity createConnection() {
        var entity = new ModelConnectionEntity();
        entity.setId(30L);
        entity.setTenantId(900001L);
        entity.setModelId(20L);
        entity.setVersion(0L);
        entity.setDataStatus(BasicConstant.DataStatus.ENABLED_CODE);
        entity.setProtocol(ModelClientProtocol.OPENAI_COMPATIBLE);
        entity.setAuthType(ModelProtocol.AUTH_API_KEY);
        entity.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
        return entity;
    }

    /** 模拟平台模型目录，使用小维度输出验证认证链，无需依赖向量数据库。 */
    private ModelEntity createModel() {
        var entity = new ModelEntity();
        entity.setVersion(0L);
        entity.setDataStatus(BasicConstant.DataStatus.ENABLED_CODE);
        entity.setModelType(ModelType.EMBEDDING);
        entity.setModelCode("test-embedding");
        return entity;
    }

    /** 仅密文及密钥标识进入连接实体，测试调用不能直接把明文传给框架。 */
    private void encryptCredential(String secret) {
        String encrypted = cipher.encrypt(secret, connection.getTenantId(), connection.getId());
        connection.setApiKeyCiphertext(encrypted);
        connection.setCredentialKeyId(cipher.requireKeyId());
    }

    /** 在HTTP边界捕获实际认证头，并返回原生SDK能够解析的最小合法响应。 */
    private void respond(HttpExchange exchange) throws IOException {
        try (exchange) {
            authorizationHeaders.add(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.getRequestBody().readAllBytes();
            String body = """
                    {"object":"list","model":"test-embedding",
                     "data":[{"object":"embedding","index":0,"embedding":[1.0,0.0]}],
                     "usage":{"prompt_tokens":1,"total_tokens":1}}
                    """;
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
    }
}
