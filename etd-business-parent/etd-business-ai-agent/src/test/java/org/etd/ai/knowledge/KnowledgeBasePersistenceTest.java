package org.etd.ai.knowledge;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.etd.ai.knowledge.entity.KnowledgeBaseEntity;
import org.etd.ai.knowledge.mapper.*;
import org.etd.ai.knowledge.service.*;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.starter.mybaits.EtdMyBatisPlusProperties;
import org.etd.framework.starter.mybaits.MybatisConfig;
import org.etd.framework.starter.mybaits.fill.handler.extend.DefaultDataFillHandler;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import java.util.*;
import java.sql.SQLException;
import org.etd.framework.starter.mybaits.permission.aspect.DataPermissionAspect;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;
import org.etd.ai.knowledge.mapper.KnowledgeBaseMapper;
import org.etd.ai.knowledge.mapper.KnowledgeChunkMapper;
import org.etd.ai.knowledge.mapper.KnowledgeDocumentMapper;
import org.etd.ai.knowledge.mapper.KnowledgeFolderMapper;
import org.etd.ai.knowledge.service.KnowledgeBaseService;
import org.etd.ai.knowledge.service.KnowledgeChunkService;
import org.etd.ai.knowledge.service.KnowledgeDocumentService;
import org.etd.ai.knowledge.service.KnowledgeFolderService;
import org.springframework.core.io.Resource;
import static org.assertj.core.api.Assertions.*;

/** 仅在新建隔离PostgreSQL库执行生产DDL及真实拦截链，验证资源独立权限、公开边界及行锁。 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(named = "AI_KNOWLEDGE_TEST_JDBC_URL", matches = ".+")
class KnowledgeBasePersistenceTest {
    private JdbcTemplate jdbc;
    private SqlSessionFactory factory;
    private SqlSession session;
    private final StaticApplicationContext application = new StaticApplicationContext();
    /** 执行完整最终DDL，拒绝在已有业务表的库内初始化。 */
    @BeforeAll
    void initialize() throws Exception {
        DriverManagerDataSource source = new DriverManagerDataSource();
        source.setUrl(System.getenv("AI_KNOWLEDGE_TEST_JDBC_URL"));
        source.setUsername(System.getenv("AI_KNOWLEDGE_TEST_DB_USER"));
        source.setPassword(System.getenv("AI_KNOWLEDGE_TEST_DB_PASSWORD"));
        jdbc = new JdbcTemplate(source);
        Long existing = jdbc.queryForObject("select count(*) from pg_tables where schemaname = 'public'", Long.class);
        assertThat(existing).isZero();
        ClassPathResource resource = new ClassPathResource("sql/ai.sql");
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(resource);
        populator.execute(source);
        MybatisSqlSessionFactoryBean bean = new MybatisSqlSessionFactoryBean();
        bean.setDataSource(source);
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        bean.setConfiguration(configuration);
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:mapper/knowledge/*.xml");
        bean.setMapperLocations(resources);
        GlobalConfig global = new GlobalConfig();
        DefaultDataFillHandler fill = new DefaultDataFillHandler();
        fill.setApplicationContext(application);
        global.setMetaObjectHandler(fill);
        bean.setGlobalConfig(global);
        EtdMyBatisPlusProperties properties = new EtdMyBatisPlusProperties();
        properties.getDataPermission().setEnabled(true);
        properties.getTenant().setEnabled(true);
        properties.getTenant().setIgnoreTables(List.of("ai_model"));
        MybatisConfig framework = new MybatisConfig(properties);
        MybatisPlusInterceptor interceptor = framework.mybatisPlusInterceptor();
        bean.setPlugins(interceptor);
        factory = bean.getObject();
    }
    /** 每例独立内容；私有文档不能因知识库公开而泄露。 */
    @BeforeEach
    void prepare() {
        jdbc.execute("truncate ai_vector_768, ai_vector_1024, ai_vector_1536, ai_knowledge_chunk, ai_knowledge_document, ai_knowledge_folder, ai_knowledge_base");
        jdbc.execute("""
                insert into ai_knowledge_base (id, tenant_id, org_id, create_by, name, is_public)
                values (1, 900001, 10, 100, '私有库', false), (2, 900001, 20, 200, '公开库', true),
                       (3, 900002, 30, 300, '其他租户库', true)
                """);
        jdbc.execute("""
                insert into ai_knowledge_folder (id, tenant_id, knowledge_base_id, folder_name)
                values (1, 900001, 1, '分类'), (2, 900001, 2, '分类'), (3, 900002, 3, '分类')
                """);
        insertDocument(1, 1, false, 0);
        insertDocument(2, 1, false, 1);
        insertDocument(3, 2, false, 1);
        insertDocument(4, 2, true, 1);
        insertDocument(5, 3, true, 1);
        UserDetails user = new UserDetails();
        user.setId(100L);
        user.setPermissionTypes(Set.of(BasicConstant.PermissionType.ORGANIZATION.getCode()));
        user.setScopeOrgIds(Set.of(10L));
        user.setOrgId(10L);
        RequestContext.setTenantCode(900001L);
        RequestContext.setUser(user);
        session = factory.openSession(true);
    }
    /** 清理会话与可信身份。 */
    @AfterEach
    void cleanup() { session.close(); RequestContext.clean(); }
    /** 目录继承库权限，文档使用自身归属；公开库中的私有文档不可读。 */
    @Test
    void shouldApplyResourcePermissionsAndProtectPrivateDocuments() {
        KnowledgeBaseService bases = createBaseService(session);
        KnowledgeDocumentService documents = createDocumentService(session);
        KnowledgeFolderMapper folderMapper = session.getMapper(KnowledgeFolderMapper.class);
        KnowledgeFolderService folderService = new KnowledgeFolderService(folderMapper);
        KnowledgeFolderService folders = permissionProxy(folderService);
        assertThat(bases.fetchBasePage(1, 12, null).getRecords()).extracting("id").containsExactly(2L, 1L);
        assertThat(documents.requireDocument(2L, false).getWritable()).isTrue();
        assertThat(documents.requireDocument(4L, false).getWritable()).isFalse();
        assertThatThrownBy(() -> documents.requireDocument(3L, false)).isInstanceOf(ApiRuntimeException.class);
        assertThatThrownBy(() -> documents.requireDocument(5L, false)).isInstanceOf(ApiRuntimeException.class);
        assertThat(folders.requireWritable(1L).getKnowledgeBaseId()).isEqualTo(1L);
        assertThatThrownBy(() -> folders.requireWritable(2L)).isInstanceOf(ApiRuntimeException.class);
        assertThatThrownBy(() -> folders.requireFolderByBase(1L, 2L)).isInstanceOf(ApiRuntimeException.class);
        RequestContext.getUser().setPlatformAdmin(true);
        assertThat(documents.requireDocument(5L, false)).isNotNull();
    }
    /** 本人范围分别按知识库和资料创建人判断，分片继承资料归属。 */
    @Test
    void shouldApplySelfScopeToEachResource() {
        RequestContext.getUser().setPermissionTypes(Set.of(BasicConstant.PermissionType.SELF.getCode()));
        KnowledgeDocumentService documents = createDocumentService(session);
        KnowledgeFolderService folders = createFolderService(session);
        assertThatThrownBy(() -> documents.requireDocument(1L, true)).isInstanceOf(ApiRuntimeException.class);
        RequestContext.getUser().setId(999L);
        session.clearCache();
        assertThat(documents.requireDocument(1L, true).getWritable()).isTrue();
        RequestContext.getUser().setId(100L);
        session.clearCache();
        assertThat(folders.fetchFolderList(1L, null)).extracting("id").containsExactly(1L);
        KnowledgeChunkMapper chunkMapper = session.getMapper(KnowledgeChunkMapper.class);
        KnowledgeChunkService chunkService = new KnowledgeChunkService(chunkMapper);
        KnowledgeChunkService chunks = permissionProxy(chunkService);
        assertThat(chunks.fetchChunkList(1L)).isEmpty();
        RequestContext.getUser().setId(888L);
        session.clearCache();
        assertThatThrownBy(() -> documents.requireDocument(1L, false)).isInstanceOf(ApiRuntimeException.class);
        assertThat(folders.fetchFolderList(1L, null)).isEmpty();
    }

    /** 无角色范围只展示公开知识库；公开资料可以独立读取，但不会公开所属私有库。 */
    @Test
    void shouldKeepPublicReadSeparateFromMaintenanceScope() {
        RequestContext.getUser().setPermissionTypes(Set.of());
        jdbc.update("update ai_knowledge_document set is_public = true where id = 2");
        KnowledgeBaseService bases = createBaseService(session);
        KnowledgeDocumentService documents = createDocumentService(session);
        KnowledgeFolderService folders = createFolderService(session);
        List<KnowledgeBaseEntity> cards = bases.fetchBasePage(1, 12, null).getRecords();
        assertThat(cards).extracting("id").containsExactly(2L);
        assertThat(cards).extracting("documentCount").containsExactly(1L);
        assertThat(cards).extracting("writable").containsOnly(false);
        assertThat(documents.requireDocument(2L, false).getWritable()).isFalse();
        assertThatThrownBy(() -> documents.requireDocument(2L, true)).isInstanceOf(ApiRuntimeException.class);
        assertThatThrownBy(() -> bases.requireBase(1L, true, false)).isInstanceOf(ApiRuntimeException.class);
        assertThatThrownBy(() -> folders.requireWritable(1L)).isInstanceOf(ApiRuntimeException.class);
        assertThat(folders.fetchFolderList(1L, null)).isEmpty();
        assertThat(folders.fetchFolderList(3L, null)).isEmpty();
    }

    /** 修改知识库使用框架严格写范围，不能因为公开可读就更新其他组织配置。 */
    @Test
    void shouldEnforceFrameworkScopeForBaseUpdates() {
        KnowledgeBaseService bases = createBaseService(session);
        KnowledgeBaseEntity base = bases.requireBase(2L, false, false);
        base.setName("未授权修改");
        assertThatThrownBy(() -> bases.modifyBase(base)).isInstanceOf(ApiRuntimeException.class);
        assertThat(jdbc.queryForObject("select name from ai_knowledge_base where id = 2", String.class)).isEqualTo("公开库");
        RequestContext.getUser().setPermissionTypes(Set.of(BasicConstant.PermissionType.ALL.getCode()));
        session.clearCache();
        assertThat(bases.requireBase(2L, true, false).getWritable()).isTrue();
        assertThatThrownBy(() -> bases.requireBase(3L, false, false)).isInstanceOf(ApiRuntimeException.class);
    }

    /** MyBatis真实自动填充必须与DDL默认禁用一致，不能被基类默认启用覆盖。 */
    @Test
    void shouldFillNewDocumentAsDisabled() {
        KnowledgeDocumentMapper mapper = session.getMapper(KnowledgeDocumentMapper.class);
        KnowledgeDocumentEntity document = mapper.selectById(1L);
        document.setId(99L);
        document.setDataStatus(null);
        mapper.insert(document);
        assertThat(document.getDataStatus()).isZero();
        assertThat(jdbc.queryForObject("select data_status from ai_knowledge_document where id = 99", Integer.class)).isZero();
    }

    /** 实际执行行锁SQL；目标行被锁住，关联知识库或模型表不应被扩大加锁。 */
    @Test
    void shouldLockOnlyRequestedBaseOrDocument() throws Exception {
        try (SqlSession locked = factory.openSession(false);
             Connection probe = jdbc.getDataSource().getConnection()) {
            // 独立会话不经过Spring事务代理，显式关闭JDBC自动提交以持有行锁。
            Connection connection = locked.getConnection();
            connection.setAutoCommit(false);
            KnowledgeBaseService bases = createBaseService(locked);
            assertThat(bases.requireBase(1L, true, true)).isNotNull();
            try (Statement statement = probe.createStatement()) {
                statement.execute("set lock_timeout = '200ms'");
                assertThatThrownBy(() -> statement.executeUpdate("update ai_knowledge_base set name = name where id = 1"))
                        .isInstanceOf(SQLException.class).hasMessageContaining("lock timeout");
                connection.rollback();
                locked.clearCache();
                KnowledgeDocumentService documents = createDocumentService(locked);
                assertThat(documents.requireDocument(1L, true)).isNotNull();
                assertThat(statement.executeUpdate("update ai_knowledge_base set name = name where id = 1")).isEqualTo(1);
                assertThatThrownBy(() -> statement.executeUpdate("update ai_knowledge_document set document_name = document_name where id = 1"))
                        .isInstanceOf(SQLException.class).hasMessageContaining("lock timeout");
            } finally {
                connection.rollback();
            }
        }
    }

    /** 复用生产切面，验证Service注解确实驱动框架SQL注入。 */
    private <T> T permissionProxy(T service) {
        AspectJProxyFactory proxy = new AspectJProxyFactory(service);
        DataPermissionAspect aspect = new DataPermissionAspect();
        proxy.addAspect(aspect);
        return proxy.getProxy();
    }

    /** 构造带权限切面的知识库服务。 */
    private KnowledgeBaseService createBaseService(SqlSession sqlSession) {
        KnowledgeBaseMapper mapper = sqlSession.getMapper(KnowledgeBaseMapper.class);
        KnowledgeBaseService service = new KnowledgeBaseService(mapper);
        return permissionProxy(service);
    }

    /** 构造带权限切面的分类服务。 */
    private KnowledgeFolderService createFolderService(SqlSession sqlSession) {
        KnowledgeFolderMapper mapper = sqlSession.getMapper(KnowledgeFolderMapper.class);
        KnowledgeFolderService service = new KnowledgeFolderService(mapper);
        return permissionProxy(service);
    }

    /** 构造带权限切面的文档服务。 */
    private KnowledgeDocumentService createDocumentService(SqlSession sqlSession) {
        KnowledgeDocumentMapper mapper = sqlSession.getMapper(KnowledgeDocumentMapper.class);
        KnowledgeDocumentService service = new KnowledgeDocumentService(mapper);
        return permissionProxy(service);
    }

    private void insertDocument(long id, long base, boolean shared, int status) {
        long tenant = base == 3 ? 900002 : 900001;
        jdbc.update("""
                insert into ai_knowledge_document
                    (id, knowledge_base_id, tenant_id, org_id, create_by, folder_id, document_name, original_filename,
                     file_type, storage_bucket, storage_object_key, content_type, file_size, file_sha256,
                     part_size_bytes, part_count, upload_expires_at, upload_status, uploaded_time, is_public,
                     data_status, chunk_status, chunk_confirmed_time, chunk_count)
                values (?, ?, ?, ?, 999, ?, '测试资料', 'sample.md', 'md', 'test', 'test', 'text/markdown',
                        100, ?, 10485760, 1, current_timestamp + interval '1 day', 'completed', current_timestamp,
                        ?, ?, 'confirmed', current_timestamp, 1)
                """, id, base, tenant, base == 1 ? 10 : 20, base, "0".repeat(64), shared, status);
        jdbc.update("""
                insert into ai_knowledge_chunk (id, knowledge_base_id, tenant_id, org_id, create_by, document_id, chunk_index, content, token_count, source)
                values (?, ?, ?, 10, 999, ?, 0, '设备安装指南', 4, '{"headings":[],"pageNumbers":[],"documentItemRefs":[]}')
                """, id, base, tenant, id);
    }
}
