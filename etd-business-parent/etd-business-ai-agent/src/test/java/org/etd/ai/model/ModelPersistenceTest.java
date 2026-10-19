package org.etd.ai.model;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.session.SqlSessionFactory;
import org.etd.ai.model.component.ModelCredentialCipher;
import org.etd.ai.model.config.ModelCredentialProperties;
import org.etd.ai.model.entity.ModelConnectionEntity;
import org.etd.ai.model.entity.ModelEntity;
import org.etd.ai.model.mapper.ModelConnectionMapper;
import org.etd.ai.model.mapper.ModelMapper;
import org.etd.ai.model.service.ModelConnectionService;
import org.etd.ai.model.service.ModelService;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.starter.mybaits.EtdMyBatisPlusProperties;
import org.etd.framework.starter.mybaits.MybatisConfig;
import org.etd.framework.starter.mybaits.fill.handler.extend.DefaultDataFillHandler;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/** 加载真实模型DDL、Mapper和应用隔离配置，验证目录共享及连接凭证边界。 */
class ModelPersistenceTest {
    /** 为框架自动填充SpEL提供上下文。 */
    private final StaticApplicationContext context = new StaticApplicationContext();

    /** 清理线程身份，避免租户上下文污染后续用例。 */
    @AfterEach
    void clearContext() {
        RequestContext.clean();
        context.close();
    }

    /** 目录不写租户字段，所有租户共用；连接及关联查询仍受租户隔离。 */
    @Test
    void shouldShareCatalogAndIsolateConnections() throws Exception {
        var factory = createSessionFactory();
        populateIdentity();
        try (var session = factory.openSession(true)) {
            var models = session.getMapper(ModelMapper.class);
            var connections = session.getMapper(ModelConnectionMapper.class);
            var model = createModel(models);
            assertNull(model.getTenantId());
            var service = new ModelService(models);
            assertEquals(1, service.fetchPage(1, 10, "aliyun", "chat").getTotal());
            assertEquals(0, service.fetchPage(1, 10, "deepseek", null).getTotal());
            var connection = createConnection(model.getId());
            assertEquals(1, connections.insert(connection));
            assertEquals(1L, connection.getTenantId());
            var details = connections.selectConnectionDetail(connection.getId());
            assertEquals(model.getModelName(), details.getModelName());
            assertEquals("aliyun", details.getProviderCode());
            var page = new Page<ModelConnectionEntity>(1, 10);
            assertEquals(1, connections.selectConnectionPage(page, model.getId(), null).getTotal());
            RequestContext.setTenantCode(2L);
            assertNotNull(service.requireModel(model.getId()));
            assertNull(connections.selectConnectionDetail(connection.getId()));
            assertNull(connections.selectById(connection.getId()));
            assertEquals(0, connections.selectConnectionPage(page, null, null).getTotal());
            RequestContext.getUser().setPlatformAdmin(true);
            assertNotNull(connections.selectConnectionDetail(connection.getId()));
        }
    }

    /** 凭证真正加密；留空保留、none清空，版本冲突不覆盖，连接不可切换模型。 */
    @Test
    void shouldEncryptPreserveClearCredentialsAndRejectConflicts() throws Exception {
        var factory = createSessionFactory();
        populateIdentity();
        try (var session = factory.openSession(true)) {
            var models = session.getMapper(ModelMapper.class);
            var model = createModel(models);
            var mapper = session.getMapper(ModelConnectionMapper.class);
            var cipher = createCipher();
            var service = new ModelConnectionService(mapper, cipher);
            verifyCredentialLifecycle(service, mapper, model.getId());
        }
    }

    /** 验证新增密文、空值保留和乐观锁拒绝覆盖。 */
    private void verifyCredentialLifecycle(ModelConnectionService service, ModelConnectionMapper mapper,
            Long modelId) {
        var connection = createConnection(modelId);
        connection.setAuthType("api_key");
        var id = service.createConnection(connection, "test-only-input");
        var stored = mapper.selectById(id);
        assertNotEquals("test-only-input", stored.getApiKeyCiphertext());
        assertTrue(stored.getApiKeyCiphertext().startsWith("v1."));
        var edit = createConnection(modelId);
        edit.setId(id);
        edit.setAuthType("api_key");
        service.modifyConnection(edit, null, stored.getVersion());
        assertEquals(stored.getApiKeyCiphertext(), mapper.selectById(id).getApiKeyCiphertext());
        var stale = createConnection(modelId);
        stale.setId(id);
        assertThrows(ApiRuntimeException.class, () -> service.modifyConnection(stale, null, 0L));
        verifyCredentialClearing(service, mapper, modelId, id);
    }

    /** none必须清空存储列；不能把已有连接切到另一个模型。 */
    private void verifyCredentialClearing(ModelConnectionService service, ModelConnectionMapper mapper,
            Long modelId, Long id) {
        var latest = mapper.selectById(id);
        var clear = createConnection(modelId);
        clear.setId(id);
        service.modifyConnection(clear, null, latest.getVersion());
        assertNull(mapper.selectById(id).getApiKeyCiphertext());
        assertNull(mapper.selectById(id).getCredentialKeyId());
        clear.setModelId(modelId + 1);
        assertThrows(ApiRuntimeException.class, () -> service.modifyConnection(clear, null, clear.getVersion()));
    }

    /** 停用目录不能继续被选择，已建立连接的展示信息仍可读取。 */
    @Test
    void shouldRejectDisabledCatalogForNewConnections() throws Exception {
        var factory = createSessionFactory();
        populateIdentity();
        try (var session = factory.openSession(true)) {
            var mapper = session.getMapper(ModelMapper.class);
            var model = createModel(mapper);
            model.setDataStatus(0);
            mapper.updateById(model);
            var service = new ModelService(mapper);
            assertEquals(0, service.fetchPage(1, 10, null, null).getTotal());
            assertThrows(ApiRuntimeException.class, () -> service.requireModel(model.getId()));
        }
    }

    /** 使用真实YAML、填充器和拦截链，避免测试自行复制隔离策略。 */
    private SqlSessionFactory createSessionFactory() throws Exception {
        var source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:models" + System.nanoTime() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        createTables(source);
        var factory = new MybatisSqlSessionFactoryBean();
        var configuration = new MybatisConfiguration();
        factory.setConfiguration(configuration);
        factory.setDataSource(source);
        var resource = new ClassPathResource("mapper/model/ModelConnectionMapper.xml");
        factory.setMapperLocations(resource);
        var global = new GlobalConfig();
        var fill = new DefaultDataFillHandler();
        fill.setApplicationContext(context);
        global.setMetaObjectHandler(fill);
        factory.setGlobalConfig(global);
        var properties = loadProperties();
        var framework = new MybatisConfig(properties);
        var interceptor = framework.mybatisPlusInterceptor();
        factory.setPlugins(interceptor);
        var sessionFactory = factory.getObject();
        sessionFactory.getConfiguration().addMapper(ModelMapper.class);
        return sessionFactory;
    }

    /** 直接执行生产DDL中的两张模型表定义，不在测试另建近似结构。 */
    private void createTables(JdbcDataSource source) throws Exception {
        var resource = new ClassPathResource("sql/ai.sql");
        var ddl = resource.getContentAsString(StandardCharsets.UTF_8);
        try (var connection = source.getConnection(); var statement = connection.createStatement()) {
            for (var table : new String[]{"ai_model", "ai_model_connection"}) {
                int start = ddl.indexOf("create table " + table + "\n");
                int end = ddl.indexOf(";", start);
                // H2使用Java正则，替换等价的空白字符类；完整原始DDL另用PostgreSQL验证。
                var tableDdl = ddl.substring(start, end).replace("[^[:space:]]", "[^\\s]");
                statement.execute(tableDdl);
            }
        }
    }

    /** 与生产资源相同的全局目录忽略表规则。 */
    private EtdMyBatisPlusProperties loadProperties() {
        var yaml = new YamlPropertiesFactoryBean();
        var resource = new ClassPathResource("etd-ai-agent.yml");
        yaml.setResources(resource);
        var source = new PropertiesPropertySource("ai-model-test", yaml.getObject());
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(source);
        return Binder.get(environment).bind("etd.mybatis", EtdMyBatisPlusProperties.class).get();
    }

    /** 平台模型的维护归属不带租户，测试插入模拟开发人员录入。 */
    private ModelEntity createModel(ModelMapper mapper) {
        var model = new ModelEntity();
        model.setProviderCode("aliyun");
        model.setModelName("测试模型");
        model.setModelCode("test-model");
        model.setModelType("chat");
        model.setSort(0);
        assertEquals(1, mapper.insert(model));
        return model;
    }

    /** 创建只包含模型关联和接入配置的连接，无连接器名称和模型定义副本。 */
    private ModelConnectionEntity createConnection(Long modelId) {
        var connection = new ModelConnectionEntity();
        connection.setModelId(modelId);
        connection.setProtocol("ollama");
        connection.setAuthType("none");
        connection.setBaseUrl("http://127.0.0.1:11434");
        return connection;
    }

    /** 使用每次随机生成的测试主密钥，不依赖本地或生产凭证。 */
    private ModelCredentialCipher createCipher() {
        var properties = new ModelCredentialProperties();
        var key = new byte[32];
        var random = new SecureRandom();
        random.nextBytes(key);
        properties.setKey(Base64.getEncoder().encodeToString(key));
        properties.setKeyId("test-key");
        return new ModelCredentialCipher(properties);
    }

    /** 普通租户身份用于验证目录共享与连接隔离。 */
    private void populateIdentity() {
        var user = new UserDetails();
        user.setId(100L);
        RequestContext.setUser(user);
        RequestContext.setTenantCode(1L);
    }
}
