package org.etd.ai.agent;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.etd.ai.agent.biz.AgentBizService;
import org.etd.ai.agent.entity.AgentEntity;
import org.etd.ai.agent.mapper.AgentMapper;
import org.etd.ai.agent.model.AgentConfiguration;
import org.etd.ai.agent.model.AgentModelOptions;
import org.etd.ai.agent.service.AgentService;
import org.etd.ai.knowledge.mapper.KnowledgeBaseMapper;
import org.etd.ai.knowledge.service.KnowledgeBaseService;
import org.etd.ai.model.component.ModelCredentialCipher;
import org.etd.ai.model.mapper.ModelConnectionMapper;
import org.etd.ai.model.mapper.ModelMapper;
import org.etd.ai.model.service.ModelConnectionService;
import org.etd.ai.model.service.ModelService;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.starter.mybaits.EtdMyBatisPlusProperties;
import org.etd.framework.starter.mybaits.MybatisConfig;
import org.etd.framework.starter.mybaits.fill.handler.extend.DefaultDataFillHandler;
import org.etd.framework.starter.mybaits.permission.aspect.DataPermissionAspect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/** 仅在隔离空PostgreSQL库加载完整生产DDL，验证Agent页面用例、JSONB及真实权限拦截链。 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(named = "AI_AGENT_TEST_JDBC_URL", matches = ".+")
class AgentPersistenceTest {
    /** 隔离数据库的种子数据入口。 */
    private JdbcTemplate jdbc;
    /** 使用真实XML、填充及租户/组织/乐观锁插件的会话工厂。 */
    private SqlSessionFactory sessions;
    /** 当前测试事务，测试结束回滚。 */
    private SqlSession session;
    /** Agent数据权限代理。 */
    private AgentService agents;
    /** 真实模型连接选择能力。 */
    private ModelConnectionService connections;
    /** 知识库数据权限代理。 */
    private KnowledgeBaseService bases;
    /** 真实业务编排，事务由测试会话显式维持。 */
    private AgentBizService biz;
    /** 配置约束使用的实际校验工厂。 */
    private ValidatorFactory validators;
    /** 供框架填充表达式解析使用的最小Spring上下文。 */
    private final StaticApplicationContext application = new StaticApplicationContext();

    /** 拒绝在已有业务表的数据库执行初始化，并加载完整生产脚本。 */
    @BeforeAll
    void initialize() throws Exception {
        var source = new DriverManagerDataSource();
        source.setUrl(System.getenv("AI_AGENT_TEST_JDBC_URL"));
        source.setUsername(System.getenv("AI_AGENT_TEST_DB_USER"));
        source.setPassword(System.getenv("AI_AGENT_TEST_DB_PASSWORD"));
        jdbc = new JdbcTemplate(source);
        Long tableCount = jdbc.queryForObject("select count(*) from pg_tables where schemaname = 'public'", Long.class);
        assertThat(tableCount).isZero();
        var resource = new ClassPathResource("sql/ai.sql");
        var populator = new ResourceDatabasePopulator(resource);
        populator.execute(source);
        sessions = createSessionFactory(source);
        validators = Validation.buildDefaultValidatorFactory();
    }

    /** 工厂使用生产插件，避免仅靠模拟Mapper掩盖JSONB映射或权限SQL错误。 */
    private SqlSessionFactory createSessionFactory(DriverManagerDataSource source) throws Exception {
        var bean = new MybatisSqlSessionFactoryBean();
        bean.setDataSource(source);
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        bean.setConfiguration(configuration);
        var resolver = new PathMatchingResourcePatternResolver();
        var resources = resolver.getResources("classpath*:mapper/**/*.xml");
        bean.setMapperLocations(resources);
        var global = new GlobalConfig();
        var fill = new DefaultDataFillHandler();
        fill.setApplicationContext(application);
        global.setMetaObjectHandler(fill);
        bean.setGlobalConfig(global);
        var properties = createProperties();
        var framework = new MybatisConfig(properties);
        var interceptor = framework.mybatisPlusInterceptor();
        bean.setPlugins(interceptor);
        return bean.getObject();
    }

    /** 与业务配置一致：全局模型不按租户隔离，租户连接不按组织隔离。 */
    private EtdMyBatisPlusProperties createProperties() {
        var properties = new EtdMyBatisPlusProperties();
        properties.getTenant().setEnabled(true);
        var globalTables = List.of("ai_model");
        properties.getTenant().setIgnoreTables(globalTables);
        properties.getDataPermission().setEnabled(true);
        var unscopedTables = List.of("ai_model", "ai_model_connection");
        properties.getDataPermission().setIgnoreTables(unscopedTables);
        properties.getLogicDelete().setEnabled(true);
        return properties;
    }

    /** 每例只操作隔离库，准备模型、知识库和可信用户，再开始可回滚的实际会话。 */
    @BeforeEach
    void prepare() {
        jdbc.execute("truncate ai_agent_message, ai_agent_run, ai_agent_conversation, ai_agent, ai_model_connection, ai_model, ai_knowledge_base");
        prepareModels();
        prepareKnowledgeBases();
        populateIdentity();
        session = sessions.openSession(false);
        var mapper = session.getMapper(AgentMapper.class);
        var service = new AgentService(mapper);
        agents = permissionProxy(service);
        var connectionMapper = session.getMapper(ModelConnectionMapper.class);
        var cipher = mock(ModelCredentialCipher.class);
        connections = new ModelConnectionService(connectionMapper, cipher);
        var modelMapper = session.getMapper(ModelMapper.class);
        var models = new ModelService(modelMapper);
        var baseMapper = session.getMapper(KnowledgeBaseMapper.class);
        var baseService = new KnowledgeBaseService(baseMapper);
        bases = permissionProxy(baseService);
        biz = new AgentBizService(agents, connections, models, bases, validators.getValidator());
    }

    /** 种子连接覆盖可用、停用、跨租户、类型不匹配及停用模型。 */
    private void prepareModels() {
        jdbc.execute("""
                insert into ai_model (id, provider_code, model_type, model_name, model_code, embedding, data_status)
                values (10, 'test', 'chat', '聊天模型', 'chat-test', null, 1),
                       (11, 'test', 'embedding', '向量模型', 'embedding-test', array[768], 1),
                       (12, 'test', 'chat', '停用模型', 'disabled-test', null, 0);
                insert into ai_model_connection (id, tenant_id, model_id, protocol, base_url, auth_type, data_status)
                values (100, 1, 10, 'openai_compatible', 'https://example.invalid', 'none', 1),
                       (101, 1, 12, 'openai_compatible', 'https://example.invalid', 'none', 1),
                       (102, 2, 10, 'openai_compatible', 'https://example.invalid', 'none', 1),
                       (103, 1, 11, 'openai_compatible', 'https://example.invalid', 'none', 1);
                """);
    }

    /** 种子知识库覆盖组织范围、公开、停用、跨租户和无权限私有库。 */
    private void prepareKnowledgeBases() {
        jdbc.execute("""
                insert into ai_knowledge_base (id, tenant_id, org_id, create_by, name, is_public, data_status)
                values (200, 1, 10, 100, '本组织库', false, 1),
                       (201, 1, 20, 200, '公开库', true, 1),
                       (202, 1, 10, 100, '停用库', false, 0),
                       (203, 2, 10, 100, '其他租户库', true, 1),
                       (204, 1, 20, 200, '私有库', false, 1);
                """);
    }

    /** 普通用户仅有本组织范围，不使用管理员绕过权限测试。 */
    private void populateIdentity() {
        var user = new UserDetails();
        user.setId(100L);
        user.setOrgId(10L);
        var types = Set.of(BasicConstant.PermissionType.ORGANIZATION.getCode());
        user.setPermissionTypes(types);
        var orgIds = Set.of(10L);
        user.setScopeOrgIds(orgIds);
        RequestContext.setTenantCode(1L);
        RequestContext.setUser(user);
    }

    /** 验证创建、发布、草稿隔离、清空字段、版本冲突、启停和逻辑删除的完整管理链路。 */
    @Test
    void shouldManageDraftAndPublishedSnapshotWithVersions() {
        var input = createInput();
        input.setDescription("待清空简介");
        input.setAvatarPath("/test-avatar.png");
        var created = biz.createAgent(input);
        Long id = created.getId();
        assertThat(created.getVersion()).isZero();
        assertThat(created.getDataStatus()).isEqualTo(BasicConstant.DataStatus.ENABLED_CODE);
        var published = biz.publishAgent(id, 0L);
        assertThat(published.getVersion()).isEqualTo(1);
        session.clearCache();
        var loaded = agents.requireAgent(id, true, false);
        assertThat(loaded.getDraftConfig()).isEqualTo(loaded.getPublishedConfig()).isNotSameAs(loaded.getPublishedConfig());
        assertThat(loaded.getUnpublishedChanges()).isFalse();
        assertDraftIsolation(id);
        assertThatThrownBy(() -> biz.publishAgent(id, 1L)).isInstanceOf(ApiRuntimeException.class);
        var disabled = biz.modifyAgentStatus(id, BasicConstant.DataStatus.DISABLED_CODE, 2L);
        assertThat(disabled.getVersion()).isEqualTo(3);
        assertThatThrownBy(() -> biz.removeAgent(id, 2L)).isInstanceOf(ApiRuntimeException.class);
        biz.removeAgent(id, 3L);
        assertThatThrownBy(() -> agents.requireAgent(id, false, false)).isInstanceOf(ApiRuntimeException.class);
    }

    /** 草稿替换和可空字段清理不会覆盖已经发布的配置。 */
    private void assertDraftIsolation(Long id) {
        var changed = createInput();
        changed.getDraftConfig().setSystemPrompt("新的草稿指令");
        biz.modifyAgent(id, changed, 1L);
        session.clearCache();
        var updated = agents.requireAgent(id, true, false);
        assertThat(updated.getDescription()).isNull();
        assertThat(updated.getAvatarPath()).isNull();
        assertThat(updated.getDraftConfig().getSystemPrompt()).isEqualTo("新的草稿指令");
        assertThat(updated.getPublishedConfig().getSystemPrompt()).isEqualTo("回答知识问题");
        assertThat(updated.getUnpublishedChanges()).isTrue();
    }

    /** 空草稿可保存但不能发布；类型、权限、租户、启停、重复绑定和非法参数都会阻止发布或保存。 */
    @Test
    void shouldValidateSelectedReferencesAndPublicationCompleteness() {
        var empty = createInput();
        var configuration = new AgentConfiguration();
        empty.setDraftConfig(configuration);
        var created = biz.createAgent(empty);
        assertThatThrownBy(() -> biz.publishAgent(created.getId(), 0L)).isInstanceOf(ApiRuntimeException.class);
        for (long connectionId : List.of(101L, 102L, 103L)) {
            var invalid = createInput();
            invalid.getDraftConfig().setModelConnectionId(connectionId);
            assertThatThrownBy(() -> biz.createAgent(invalid)).isInstanceOf(ApiRuntimeException.class);
        }
        for (long baseId : List.of(202L, 203L, 204L)) {
            var invalid = createInput();
            var ids = List.of(baseId);
            invalid.getDraftConfig().setKnowledgeBaseIds(ids);
            assertThatThrownBy(() -> biz.createAgent(invalid)).isInstanceOf(ApiRuntimeException.class);
        }
        var duplicate = createInput();
        var repeatedIds = List.of(200L, 200L);
        duplicate.getDraftConfig().setKnowledgeBaseIds(repeatedIds);
        assertThatThrownBy(() -> biz.createAgent(duplicate)).isInstanceOf(ApiRuntimeException.class);
        var invalidOptions = createInput();
        invalidOptions.getDraftConfig().getModelOptions().setTemperature(Double.NaN);
        assertThatThrownBy(() -> biz.createAgent(invalidOptions)).isInstanceOf(ApiRuntimeException.class);
    }

    /** 公开仅扩大读取范围，不能读取编辑配置或维护；平台管理员仍保留跨租户访问能力。 */
    @Test
    void shouldSeparatePublicReadingFromMaintenanceAndTenantIsolation() {
        jdbc.execute("""
                insert into ai_agent (id, tenant_id, org_id, create_by, name, is_public, draft_config)
                values (1, 1, 10, 100, '可维护', false, '{}'),
                       (2, 1, 20, 200, '私有', false, '{}'),
                       (3, 1, 20, 200, '公开', true, '{}'),
                       (4, 2, 10, 100, '其他租户', true, '{}');
                """);
        var page = agents.fetchAgentPage(1, 20, null, null, null);
        assertThat(page.getRecords()).extracting(AgentEntity::getId).containsExactlyInAnyOrder(1L, 3L);
        assertThat(agents.requireAgent(3L, false, false).getWritable()).isFalse();
        assertThatThrownBy(() -> agents.requireAgent(3L, true, false)).isInstanceOf(ApiRuntimeException.class);
        assertThatThrownBy(() -> agents.requireAgent(2L, false, false)).isInstanceOf(ApiRuntimeException.class);
        assertThatThrownBy(() -> agents.requireAgent(4L, false, false)).isInstanceOf(ApiRuntimeException.class);
        var input = createInput();
        input.setOrgId(20L);
        assertThatThrownBy(() -> biz.createAgent(input)).isInstanceOf(ApiRuntimeException.class);
        RequestContext.getUser().setPlatformAdmin(true);
        session.clearCache();
        assertThat(agents.requireAgent(4L, true, false).getTenantId()).isEqualTo(2L);
        var crossTenant = createInput();
        crossTenant.getDraftConfig().setModelConnectionId(102L);
        assertThatThrownBy(() -> biz.createAgent(crossTenant)).isInstanceOf(ApiRuntimeException.class);
    }

    /** 选择接口SQL在分页前过滤停用模型、非聊天模型及无权限知识库。 */
    @Test
    void shouldFilterOptionsInDatabase() {
        var modelPage = connections.fetchChatConnectionPage(1, 20, null);
        assertThat(modelPage.getTotal()).isEqualTo(1);
        assertThat(modelPage.getRecords()).extracting(value -> value.getId()).containsExactly(100L);
        assertThat(modelPage.getRecords().getFirst().getBaseUrl()).isNull();
        var basePage = bases.fetchAvailableBasePage(1, 20, null);
        assertThat(basePage.getTotal()).isEqualTo(2);
        assertThat(basePage.getRecords()).extracting(value -> value.getId()).containsExactlyInAnyOrder(200L, 201L);
        var selected = bases.fetchAvailableBasePage(1, 1, "公开");
        assertThat(selected.getTotal()).isEqualTo(1);
        assertThat(selected.getRecords().getFirst().getId()).isEqualTo(201L);
    }

    /** 构造完整可运行草稿，集合可变以验证发布副本隔离。 */
    private AgentEntity createInput() {
        var input = new AgentEntity();
        input.setOrgId(10L);
        input.setName(" 知识助手 ");
        input.setIsPublic(false);
        var configuration = new AgentConfiguration();
        configuration.setModelConnectionId(100L);
        configuration.setSystemPrompt("回答知识问题");
        var ids = new ArrayList<Long>();
        ids.add(200L);
        configuration.setKnowledgeBaseIds(ids);
        var options = new AgentModelOptions();
        options.setTemperature(0.5);
        configuration.setModelOptions(options);
        input.setDraftConfig(configuration);
        return input;
    }

    /** 与生产一致地进入数据权限切面，不直接模拟授权结果。 */
    private <T> T permissionProxy(T service) {
        var proxy = new AspectJProxyFactory(service);
        var aspect = new DataPermissionAspect();
        proxy.addAspect(aspect);
        return proxy.getProxy();
    }

    /** 回滚测试写入并清理线程身份，避免污染其他测试。 */
    @AfterEach
    void cleanup() {
        session.rollback();
        session.close();
        RequestContext.clean();
    }

    /** 释放校验和表达式上下文资源。 */
    @AfterAll
    void closeResources() {
        validators.close();
        application.close();
    }
}
