package org.etd.ai.knowledge;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.etd.ai.knowledge.biz.KnowledgeRecallBizService;
import org.etd.ai.knowledge.biz.KnowledgeIndexBuildBizService;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.etd.ai.knowledge.component.KnowledgeModelClient;
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
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 仅在新建隔离PostgreSQL库执行生产DDL及真实拦截链，验证全文、向量和公开边界。 */
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
        var source = new DriverManagerDataSource();
        source.setUrl(System.getenv("AI_KNOWLEDGE_TEST_JDBC_URL"));
        source.setUsername(System.getenv("AI_KNOWLEDGE_TEST_DB_USER"));
        source.setPassword(System.getenv("AI_KNOWLEDGE_TEST_DB_PASSWORD"));
        jdbc = new JdbcTemplate(source);
        var existing = jdbc.queryForObject("select count(*) from pg_tables where schemaname = 'public'", Long.class);
        assertThat(existing).isZero();
        var resource = new ClassPathResource("sql/ai.sql");
        var populator = new ResourceDatabasePopulator(resource);
        populator.execute(source);
        var bean = new MybatisSqlSessionFactoryBean();
        bean.setDataSource(source);
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        bean.setConfiguration(configuration);
        var resolver = new PathMatchingResourcePatternResolver();
        var resources = resolver.getResources("classpath*:mapper/knowledge/*.xml");
        bean.setMapperLocations(resources);
        var global = new GlobalConfig();
        var fill = new DefaultDataFillHandler();
        fill.setApplicationContext(application);
        global.setMetaObjectHandler(fill);
        bean.setGlobalConfig(global);
        var properties = new EtdMyBatisPlusProperties();
        properties.getDataPermission().setEnabled(true);
        properties.getTenant().setEnabled(true);
        properties.getTenant().setIgnoreTables(List.of("ai_model"));
        var framework = new MybatisConfig(properties);
        var interceptor = framework.mybatisPlusInterceptor();
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
        var user = new UserDetails();
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
    /** 分类继承库权限，不能依赖目录创建人；公开库中的私有文档不可读。 */
    @Test
    void shouldInheritBasePermissionsAndProtectPrivateDocuments() {
        var bases = new KnowledgeBaseService(session.getMapper(KnowledgeBaseMapper.class));
        var documents = new KnowledgeDocumentService(session.getMapper(KnowledgeDocumentMapper.class));
        var folders = new KnowledgeFolderService(session.getMapper(KnowledgeFolderMapper.class));
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
    /** 管理召回允许禁用文档，正式接口强制启用，知识库关闭后正式查询拒绝。 */
    @Test
    void shouldSeparateTestAndProductionRecall() {
        var bases = new KnowledgeBaseService(session.getMapper(KnowledgeBaseMapper.class));
        var retrieval = new KnowledgeRetrievalService(session.getMapper(KnowledgeRetrievalMapper.class));
        var models = mock(KnowledgeModelClient.class);
        var biz = new KnowledgeRecallBizService(bases, retrieval, models);
        assertThat(biz.recall(1L, "安装", null, true)).extracting("documentId").containsExactlyInAnyOrder(1L, 2L);
        assertThat(biz.recall(1L, "安装", 0, false)).extracting("documentId").containsExactly(2L);
        assertThat(biz.recall(2L, "安装", null, false)).extracting("documentId").containsExactly(4L);
        jdbc.update("update ai_knowledge_document set data_status = 0 where id = 2");
        session.clearCache();
        assertThat(biz.recall(1L, "安装", null, false)).isEmpty();
        jdbc.update("update ai_knowledge_base set data_status = 0 where id = 1");
        session.clearCache();
        assertThatThrownBy(() -> biz.recall(1L, "安装", null, false)).isInstanceOf(ApiRuntimeException.class);
    }
    /** 相同维度必须匹配知识库、模型、索引和正文版本；停用实时生效且不删除向量。 */
    @Test
    void shouldFilterVectorGenerationAndPublicationState() throws Exception {
        jdbc.update("update ai_knowledge_base set retrieval_mode = 'vector', active_connection_id = 99, active_dimensions = 768, active_index_version = 10 where id = 1");
        var mapper = session.getMapper(KnowledgeRetrievalMapper.class);
        float[] embedding = new float[768];
        embedding[0] = 1;
        String vector = Arrays.toString(embedding);
        var metadata = Map.of("tenant_id", "900001", "knowledge_base_id", "1", "document_id", "2", "chunk_id", "2",
                "model_connection_id", "99", "index_version", "10", "processing_version", "0");
        var json = new ObjectMapper();
        String encoded = json.writeValueAsString(metadata);
        mapper.insertVector("ai_vector_768", 1L, "设备安装指南", encoded, vector);
        var bases = new KnowledgeBaseService(session.getMapper(KnowledgeBaseMapper.class));
        var retrieval = new KnowledgeRetrievalService(mapper);
        var models = mock(KnowledgeModelClient.class);
        when(models.embed(eq(99L), eq(768), anyString(), eq(true))).thenReturn(embedding);
        var biz = new KnowledgeRecallBizService(bases, retrieval, models);
        assertThat(biz.recall(1L, "安装", null, false)).extracting("documentId").containsExactly(2L);
        jdbc.update("update ai_knowledge_document set processing_version = 1 where id = 2");
        session.clearCache();
        assertThat(biz.recall(1L, "安装", null, false)).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from ai_vector_768", Long.class)).isEqualTo(1);
    }
    /** MyBatis真实自动填充必须与DDL默认禁用一致，不能被基类默认启用覆盖。 */
    @Test
    void shouldFillNewDocumentAsDisabled() {
        var mapper = session.getMapper(KnowledgeDocumentMapper.class);
        var document = mapper.selectById(1L);
        document.setId(99L);
        document.setDataStatus(null);
        mapper.insert(document);
        assertThat(document.getDataStatus()).isZero();
        assertThat(jdbc.queryForObject("select data_status from ai_knowledge_document where id = 99", Integer.class)).isZero();
    }

    /** 新维度后台构建后原子切换；后续模型失败不会覆盖当前生效版本。 */
    @Test
    void shouldPublishNewDimensionsAndKeepActiveIndexOnFailure() {
        jdbc.update("""
                update ai_knowledge_base set embedding_connection_id = 99, dimensions = 1024,
                    building_index_version = 10, index_status = 'building', index_lease_until = current_timestamp + interval '1 hour'
                where id = 1
                """);
        var mapper = session.getMapper(KnowledgeBaseMapper.class);
        var vectors = session.getMapper(KnowledgeRetrievalMapper.class);
        var models = mock(KnowledgeModelClient.class);
        float[] embedding = new float[1024];
        embedding[0] = 1;
        when(models.embed(eq(99L), eq(1024), anyString(), eq(false))).thenReturn(embedding);
        var manager = new DataSourceTransactionManager(jdbc.getDataSource());
        var transactions = new TransactionTemplate(manager);
        var json = new ObjectMapper();
        var worker = new KnowledgeIndexBuildBizService(mapper, vectors, models, json, transactions);
        var task = mapper.selectById(1L);
        worker.buildIndex(task);
        session.clearCache();
        var published = mapper.selectById(1L);
        assertThat(published.getActiveDimensions()).isEqualTo(1024);
        assertThat(published.getActiveIndexVersion()).isEqualTo(10);
        assertThat(published.getIndexStatus()).isEqualTo("ready");
        assertThat(jdbc.queryForObject("select count(*) from ai_vector_1024", Long.class)).isEqualTo(2);
        jdbc.update("""
                update ai_knowledge_base set embedding_connection_id = 100, dimensions = 1536,
                    building_index_version = 11, index_status = 'building', index_lease_until = current_timestamp + interval '1 hour'
                where id = 1
                """);
        when(models.embed(eq(100L), eq(1536), anyString(), eq(false))).thenThrow(ApiRuntimeException.class);
        session.clearCache();
        var failedTask = mapper.selectById(1L);
        worker.buildIndex(failedTask);
        session.clearCache();
        var failed = mapper.selectById(1L);
        assertThat(failed.getIndexStatus()).isEqualTo("failed");
        assertThat(failed.getActiveDimensions()).isEqualTo(1024);
        assertThat(failed.getActiveIndexVersion()).isEqualTo(10);
    }

    private void insertDocument(long id, long base, boolean shared, int status) {
        long tenant = base == 3 ? 900002 : 900001;
        jdbc.update("""
                insert into ai_knowledge_document
                    (id, knowledge_base_id, tenant_id, org_id, create_by, folder_id, document_name, original_filename,
                     file_type, storage_bucket, storage_object_key, content_type, file_size, file_sha256,
                     part_size_bytes, part_count, upload_expires_at, upload_status, uploaded_time, is_public,
                     data_status, chunk_status, chunk_confirmed_time, chunk_count)
                values (?, ?, ?, 10, 999, ?, '测试资料', 'sample.md', 'md', 'test', 'test', 'text/markdown',
                        100, ?, 10485760, 1, current_timestamp + interval '1 day', 'completed', current_timestamp,
                        ?, ?, 'confirmed', current_timestamp, 1)
                """, id, base, tenant, base, "0".repeat(64), shared, status);
        jdbc.update("""
                insert into ai_knowledge_chunk (id, knowledge_base_id, tenant_id, org_id, document_id, chunk_index, content, token_count, source)
                values (?, ?, ?, 10, ?, 0, '设备安装指南', 4, '{"headings":[],"pageNumbers":[],"documentItemRefs":[]}')
                """, id, base, tenant, id);
    }
}
