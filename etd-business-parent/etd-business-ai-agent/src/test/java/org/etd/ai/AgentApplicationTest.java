package org.etd.ai;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.etd.framework.starter.client.core.authentication.bearer.BearerTokenAuthentication;
import com.etd.framework.starter.client.core.constant.SecurityParameterConstant;
import com.etd.framework.starter.client.core.encrypt.impl.JwtTokenEncoder;
import com.etd.framework.starter.client.core.permission.annotation.Permission;
import com.etd.framework.starter.client.core.properties.SecurityProperties;
import com.etd.framework.starter.client.core.storage.UserLoginTokenStorage;
import org.apache.ibatis.session.SqlSessionFactory;
import org.etd.framework.common.core.constants.PermissionAction;
import org.etd.framework.common.core.constants.PermissionCode;
import org.etd.framework.common.core.user.PermissionAuthority;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.starter.storage.StorageConfiguration;
import org.etd.framework.starter.storage.core.FileDownload;
import org.etd.framework.starter.storage.core.FileStorage;
import org.etd.framework.starter.storage.core.FileUpload;
import org.etd.framework.starter.storage.core.extend.MinIoFileStorage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * 智能体应用集成冒烟测试，验证真实自动配置与健康接口，不调用外部模型或对象存储。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"logging.file=${java.io.tmpdir}/etd-ai-agent-tests",
                "spring.datasource.druid.driver-class-name=org.h2.Driver",
                "management.health.redis.enabled=false"})
@Import(AgentApplicationTest.SecurityProbeController.class)
class AgentApplicationTest {

    /** 仅供测试上下文使用的受保护接口及权限资源码，不注册到正式应用。 */
    private static final String PROBE_PATH = "/test/agent-security";
    private static final String PROBE_RESOURCE = "ai:security-probe";

    /** 同一测试用户与租户贯穿令牌签发和登录状态校验。 */
    private static final Long TEST_USER_ID = 100L;
    private static final Long TEST_TENANT_ID = 200L;

    /** 每次测试进程重新生成签名密钥，私钥始终只存在于内存中。 */
    private static final KeyPair TEST_KEYS = createTestKeyPair();

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TestRestTemplate restTemplate;

    /** 离线测试替换登录状态存储，真实过滤器仍执行验签和令牌有效性检查。 */
    @MockitoBean
    private UserLoginTokenStorage tokenStorage;

    /**
     * 动态生成仅用于离线客户端初始化的测试值，测试不携带真实凭证，也不发起外部请求。
     *
     * @param registry 测试上下文属性注册器
     */
    @DynamicPropertySource
    static void registerOfflineClientProperties(DynamicPropertyRegistry registry) {
        registry.add("AI_DASHSCOPE_API_KEY", () -> UUID.randomUUID().toString());
        registry.add("AI_STORAGE_MINIO_ENDPOINT", () -> "http://127.0.0.1:19000");
        registry.add("AI_STORAGE_MINIO_ACCESS_KEY", () -> UUID.randomUUID().toString());
        registry.add("AI_STORAGE_MINIO_SECRET_KEY", () -> UUID.randomUUID().toString());
        registry.add("AI_STORAGE_MINIO_BUCKET", () -> "agent-test");
        // 测试直接覆盖最终连接属性，正式 local 配置仍使用 PostgreSQL 的 host/port/name。
        registry.add("spring.datasource.druid.url", () -> "jdbc:h2:mem:agent-test;DB_CLOSE_DELAY=-1");
        registry.add("AI_DB_USERNAME", () -> "sa");
        registry.add("AI_DB_PASSWORD", () -> UUID.randomUUID().toString());
        // Redis 连接由公共 YAML 解析通用环境变量，测试只覆盖逻辑库以隔离运行状态。
        registry.add("REDIS_HOST", () -> "127.0.0.1");
        registry.add("REDIS_PORT", () -> 16379);
        registry.add("spring.data.redis.database", () -> 0);
        registry.add("AI_SECURITY_PUBLIC_KEY_PATH", AgentApplicationTest::writeTestPublicKey);
    }

    /**
     * 同时验证 DashScope 模型、Agent 图编排和框架存储可以在完整 Web 应用中共同初始化。
     */
    @Test
    void shouldStartAgentWithFrameworkStorage() {
        DashScopeChatModel model = applicationContext.getBean(DashScopeChatModel.class);
        ReactAgent agent = ReactAgent.builder().name("startup_probe").model(model).build();

        assertThat(agent).isNotNull();
        assertThat(applicationContext.getEnvironment().getActiveProfiles()).contains("local");
        assertThat(applicationContext.getEnvironment().getProperty("spring.data.redis.port")).isEqualTo("16379");
        assertThat(applicationContext.getEnvironment().getProperty("jetcache.remote.default.keyPrefix"))
                .isEqualTo("ai-agent:");
        assertThat(applicationContext.getEnvironment().getProperty("spring.kafka.bootstrap-servers")).isNull();
        assertThat(applicationContext.getBeansOfType(ChatModel.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(EmbeddingModel.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(ImageModel.class)).isEmpty();
        assertThat(applicationContext.getBean(SqlSessionFactory.class)).isNotNull();
        assertThat(applicationContext.getBean(FileUpload.class)).isInstanceOf(MinIoFileStorage.class);
        assertThat(applicationContext.getBean(FileDownload.class)).isInstanceOf(MinIoFileStorage.class);
        var response = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    /** 匿名请求不能访问业务接口，健康检查仍由上面的启动测试验证允许匿名访问。 */
    @Test
    void shouldRejectAnonymousBusinessRequest() {
        assertThat(restTemplate.getForEntity(PROBE_PATH, String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /** 无法解析的访问令牌应在真实 Bearer 认证过滤器中被拒绝。 */
    @Test
    void shouldRejectInvalidAccessToken() {
        assertThat(requestProtectedResource(UUID.randomUUID().toString()).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /** 有效签名及登录状态不足以代替权限码，未授权用户仍应收到 403。 */
    @Test
    void shouldRejectUserWithoutPermission() {
        String token = createAccessToken(false);
        allowLoginToken(token);
        assertThat(requestProtectedResource(token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /** 正确的签名、有效登录状态和权限码应共同允许请求到达业务接口。 */
    @Test
    void shouldAcceptAuthorizedUser() {
        String token = createAccessToken(true);
        allowLoginToken(token);
        assertThat(requestProtectedResource(token).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /** 即使 JWT 签名正确，登录状态已撤销时也必须拒绝访问。 */
    @Test
    void shouldRejectRevokedAccessToken() {
        assertThat(requestProtectedResource(createAccessToken(true)).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /** 使用框架签发器构造与 UPMS 相同格式的测试访问令牌。 */
    private String createAccessToken(boolean granted) {
        UserDetails user = new UserDetails();
        user.setId(TEST_USER_ID);
        user.setTenantId(TEST_TENANT_ID);
        user.setEnabled(true);
        user.setTenantEnabled(true);
        if (granted) {
            user.setAuthorities(List.of(new PermissionAuthority(
                    PermissionCode.createAuthority(PROBE_RESOURCE, PermissionAction.READ))));
        }
        BearerTokenAuthentication authentication = new BearerTokenAuthentication(user.getAuthorities());
        authentication.setDetails(user);
        JwtTokenEncoder encoder = new JwtTokenEncoder(TEST_KEYS.getPrivate(),
                applicationContext.getBean(SecurityProperties.class));
        return encoder.encode(SecurityParameterConstant.TokenType.ACCESS_TOKEN, authentication).getValue();
    }

    /** 只允许当前测试签发的令牌，避免把任意签名令牌视为有效登录。 */
    private void allowLoginToken(String token) {
        String userId = TEST_USER_ID.toString();
        when(tokenStorage.isAccessTokenPresent(userId)).thenReturn(true);
        when(tokenStorage.accessTokenMatches(userId, token)).thenReturn(true);
    }

    /** 通过实际 HTTP 请求验证 Security 过滤器和方法权限注解，而非直接调用 Controller。 */
    private ResponseEntity<String> requestProtectedResource(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(PROBE_PATH, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    /** 创建临时 RSA 密钥，测试不读取仓库中的密钥文件。 */
    private static KeyPair createTestKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("无法生成测试 RSA 密钥", exception);
        }
    }

    /** 写出临时公钥以验证正式公钥加载器，进程退出时删除文件。 */
    private static String writeTestPublicKey() {
        try {
            Path path = Files.createTempFile("etd-ai-test-public-", ".pem");
            String encoded = Base64.getMimeEncoder(64, new byte[]{'\n'})
                    .encodeToString(TEST_KEYS.getPublic().getEncoded());
            Files.writeString(path, "-----BEGIN PUBLIC KEY-----\n" + encoded + "\n-----END PUBLIC KEY-----\n");
            path.toFile().deleteOnExit();
            return path.toString();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("无法写入测试公钥", exception);
        }
    }

    /** 仅导入测试上下文的权限探针，不作为正式业务 HTTP API。 */
    @RestController
    static class SecurityProbeController {

        /**
         * 权限注解与正式业务接口使用相同的授权机制。
         *
         * @return 通过认证和资源权限检查后的探针响应
         */
        @GetMapping(PROBE_PATH)
        @Permission(PROBE_RESOURCE)
        public String getSecurityProbe() {
            return "authorized";
        }
    }

    /**
     * 未启用存储厂商时不应创建客户端，允许先开发智能体而无需准备对象存储服务。
     */
    @Test
    void shouldLeaveStorageDisabledByDefault() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(StorageConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(FileStorage.class));
    }
}
