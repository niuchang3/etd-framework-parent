package org.etd.ai.knowledge;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.etd.ai.document.entity.KnowledgeDocumentEntity;
import org.etd.ai.knowledge.constant.AiStage;
import org.etd.ai.document.mapper.KnowledgeDocumentMapper;
import org.etd.ai.document.service.KnowledgeDocumentService;
import org.etd.ai.knowledge.entity.KnowledgeFolderEntity;
import org.etd.ai.knowledge.mapper.KnowledgeFolderMapper;
import org.etd.ai.knowledge.mapper.KnowledgeBaseMapper;
import org.etd.ai.model.mapper.ModelConnectionMapper;
import org.etd.ai.model.service.ModelConnectionService;
import org.etd.ai.model.service.ModelConfigService;
import org.etd.ai.model.component.ModelCredentialCipher;
import org.etd.ai.knowledge.service.KnowledgeBaseService;
import org.etd.framework.starter.mybaits.fill.handler.extend.DefaultDataFillHandler;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.etd.ai.model.mapper.ModelConfigMapper;
import org.etd.ai.upload.mapper.StorageCleanupMapper;
import org.etd.ai.upload.mapper.UploadSessionMapper;
import org.etd.ai.upload.service.UploadSessionService;
import org.etd.ai.upload.entity.UploadSessionEntity;
import org.springframework.beans.BeanUtils;
import org.etd.ai.upload.entity.StorageCleanupEntity;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.etd.ai.knowledge.service.KnowledgeFolderService;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.starter.mybaits.EtdMyBatisPlusProperties;
import org.etd.framework.starter.mybaits.MybatisConfig;
import org.etd.framework.starter.mybaits.permission.aspect.DataPermissionAspect;
import org.etd.framework.starter.mybaits.permission.context.DataPermissionContextHolder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;
import javax.sql.DataSource;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.wall.WallConfig;
import com.alibaba.druid.wall.WallFilter;
import org.mybatis.spring.MyBatisSystemException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 在一次性PostgreSQL数据库内验证真实XML、Spring权限切面、分页与递归导航。 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(named = "AI_PERMISSION_TEST_DB_URL", matches = ".+")
class KnowledgeReadPermissionIntegrationTest {
    /** 知识库列表能力，复用生产Mapper验证可选状态条件。 */
    private KnowledgeBaseService baseService;
    /** 模型配置列表能力。 */
    private ModelConfigService modelService;
    /** 模型连接列表能力，不触发凭证处理。 */
    private ModelConnectionService connectionService;

    /** 上传任务能力，任务归属是独立业务规则。 */
    private UploadSessionService uploads;
    /** 真实事务模板，写测试结束时回滚以保持读测试夹具。 */
    private TransactionTemplate transactions;
    /** 仅用于测试夹具构造及结果核验，不绕过生产更新逻辑。 */
    private JdbcTemplate jdbc;
    /** 与生产一致的Druid连接池及Wall过滤器。 */
    private DruidDataSource dataSource;
    /** 审计填充使用的Spring上下文。 */
    private GenericApplicationContext fillContext;
    /** 可核验加密租户参数的凭证处理替身。 */
    private ModelCredentialCipher cipher;

    /** 真实Mapper与权限代理组成的文档查询服务。 */
    private KnowledgeDocumentService documents;
    /** 真实Mapper与权限代理组成的目录查询服务。 */
    private KnowledgeFolderService folders;

    /** 自定义模型引用统计Mapper。 */
    private ModelConnectionMapper connections;
    /** 模型配置引用统计Mapper。 */
    private ModelConfigMapper models;
    /** 跨租户存储清理Mapper。 */
    private StorageCleanupMapper cleanups;

    /** 只允许在已经加载当前schema.sql的空测试库初始化，禁止覆盖已有业务资料。 */
    @BeforeAll
    void prepareDatabaseAndServices() throws Exception {
        var source = new DruidDataSource();
        dataSource = source;
        var wall = new WallFilter();
        var wallConfig = loadSharedWallConfig();
        wall.setConfig(wallConfig);
        wall.setDbType("postgresql");
        source.setProxyFilters(List.of(wall));
        source.setDriverClassName("org.postgresql.Driver");
        source.setUrl(System.getenv("AI_PERMISSION_TEST_DB_URL"));
        source.setUsername(System.getenv("AI_PERMISSION_TEST_DB_USER"));
        source.setPassword(System.getenv("AI_PERMISSION_TEST_DB_PASSWORD"));
        jdbc = new JdbcTemplate(source);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(source));
        assertEquals(0L, jdbc.queryForObject("select count(*) from ai_knowledge_base", Long.class));
        new ResourceDatabasePopulator(new ClassPathResource(
                "sql/postgresql/knowledge-read-permission-fixtures.sql")).execute(source);
        initializeMapperServices(source);
    }

    /** 真实注册所有Mapper及生产拦截链，关联表也必须具备实体元数据。 */
    private void initializeMapperServices(DataSource source) throws Exception {
        var properties = loadApplicationMybatisProperties();
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(source);
        var configuration = new MybatisConfiguration();
        factory.setConfiguration(configuration);
        fillContext = new GenericApplicationContext();
        fillContext.refresh();
        var fill = new DefaultDataFillHandler();
        fill.setApplicationContext(fillContext);
        factory.setGlobalConfig(new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig()).setMetaObjectHandler(fill));
        factory.setPlugins(new MybatisConfig(properties).mybatisPlusInterceptor());
        var resources = new PathMatchingResourcePatternResolver();
        factory.setMapperLocations(resources.getResources("classpath*:mapper/**/*.xml"));
        var sessionFactory = factory.getObject();
        sessionFactory.getConfiguration().addMapper(KnowledgeBaseMapper.class);
        var session = new SqlSessionTemplate(sessionFactory);
        documents = createPermissionProxy(new KnowledgeDocumentService(session.getMapper(KnowledgeDocumentMapper.class)));
        folders = createPermissionProxy(new KnowledgeFolderService(session.getMapper(KnowledgeFolderMapper.class)));
        baseService = new KnowledgeBaseService(session.getMapper(KnowledgeBaseMapper.class));
        connections = session.getMapper(ModelConnectionMapper.class);
        models = session.getMapper(ModelConfigMapper.class);
        modelService = new ModelConfigService(models);
        cipher = mock(ModelCredentialCipher.class);
        connectionService = new ModelConnectionService(connections, cipher);
        cleanups = session.getMapper(StorageCleanupMapper.class);
        var uploadMapper = session.getMapper(UploadSessionMapper.class);
        uploads = new UploadSessionService(uploadMapper);
        uploadMapper.selectExpiredSessionList();
    }

    /** 角色范围为本人及组织10，不能读取组织20其他人的私有资料。 */
    @BeforeEach
    void populateLoginContext() {
        var user = new UserDetails();
        user.setId(100L);
        user.setOrgId(10L);
        user.setScopeOrgIds(Set.of(10L));
        user.setPermissionTypes(Set.of(BasicConstant.PermissionType.SELF.getCode(),
                BasicConstant.PermissionType.ORGANIZATION.getCode()));
        RequestContext.setUser(user);
        RequestContext.setTenantCode(1L);
    }

    /** 权限作用域不能残留到下一个请求。 */
    @AfterEach
    void clearContext() {
        assertNull(DataPermissionContextHolder.peek());
        DataPermissionContextHolder.clear();
        RequestContext.clean();
    }

    /** 分页总数和记录均在过滤后计算，其他租户、知识库及失效资料不能混入。 */
    @Test
    void shouldFilterDocumentPageBeforePagination() {
        var page = documents.fetchPage(1L, null, false, null, 1, 2, null);
        assertEquals(5, page.getTotal());
        assertEquals(2, page.getRecords().size());
        assertEquals(Set.of(101L, 103L, 104L, 108L, 110L), fetchDocumentIds());
    }

    /** 公开、本人和部门资料均可取得详情，私有及跨租户文件不能绕过ID校验。 */
    @Test
    void shouldFilterDocumentDetails() {
        for (long id : List.of(101L, 103L, 104L, 108L)) {
            assertEquals(id, documents.requireReadable(1L, id).getId());
        }
        for (long id : List.of(102L, 105L, 106L, 107L, 109L)) {
            assertThrows(ApiRuntimeException.class, () -> documents.requireReadable(1L, id));
        }
    }

    /** 原有目录筛选、未分类筛选和名称搜索在权限切换后保持可用。 */
    @Test
    void shouldPreserveBusinessFilters() {
        assertEquals(1, documents.fetchPage(1L, 2L, false, null, 1, 20, null).getTotal());
        assertEquals(0, documents.fetchPage(1L, 3L, false, null, 1, 20, null).getTotal());
        assertEquals(2, documents.fetchPage(1L, null, true, null, 1, 20, null).getTotal());
        assertEquals(1, documents.fetchPage(1L, null, false, "公开文件", 1, 20, null).getTotal());
    }

    /** 私有祖先及直接目录只提供导航，不公开隐藏同级目录和其中的私有文件。 */
    @Test
    void shouldPreservePrivateNavigationAncestors() {
        assertEquals(Set.of(1L, 2L, 4L, 5L, 6L), fetchFolderIds());
        assertThrows(ApiRuntimeException.class, () -> documents.requireReadable(1L, 102L));
        assertThrows(ApiRuntimeException.class, () -> folders.requireWritable(1L, 1L));
    }

    /** 没有角色范围的已登录用户仍能看到公开资料及导航路径。 */
    @Test
    void shouldAllowOnlyPublicWithoutRoleRange() {
        RequestContext.getUser().setPermissionTypes(Set.of());
        assertEquals(Set.of(101L, 108L, 110L), fetchDocumentIds());
        assertEquals(Set.of(1L, 2L, 4L), fetchFolderIds());
    }

    /** 拥有全部数据范围也只能读取当前租户、当前知识库中的有效资料。 */
    @Test
    void shouldKeepTenantBoundaryForAllRange() {
        RequestContext.getUser().setPermissionTypes(Set.of(BasicConstant.PermissionType.ALL.getCode()));
        assertEquals(Set.of(101L, 102L, 103L, 104L, 108L, 110L), fetchDocumentIds());
        assertEquals(Set.of(1L, 2L, 3L, 4L, 5L, 6L, 7L), fetchFolderIds());
    }

    /** 读取方法的公开授权不能泄漏到后续管理查询。 */
    @Test
    void shouldNotGrantWritePermissionForPublicDocuments() {
        documents.requireReadable(1L, 101L);
        assertThrows(ApiRuntimeException.class, () -> documents.requireWritable(1L, 101L, false));
        assertEquals(103L, documents.requireWritable(1L, 103L, false).getId());
        assertEquals(104L, documents.requireWritable(1L, 104L, false).getId());
    }

    /** 公开不是匿名开放，必须具备可信登录及当前租户。 */
    @Test
    void shouldRejectAnonymousRequests() {
        RequestContext.clean();
        assertThrows(ApiRuntimeException.class, () -> documents.requireReadable(1L, 101L));
        assertTrue(folders.fetchFolderList(1L, null).isEmpty());
    }

    /** 已删除知识库、目录与文档均不能因删去XML条件重新出现。 */
    @Test
    void shouldExcludeDeletedBusinessRecords() {
        assertEquals(0, documents.fetchPage(5L, null, false, null, 1, 20, null).getTotal());
        assertThrows(ApiRuntimeException.class, () -> documents.requireReadable(5L, 112L));
        assertFalse(fetchFolderIds().contains(10L));
    }

    /** 平台管理员按框架约定跨租户查询，逻辑删除过滤仍生效。 */
    @Test
    void shouldAllowPlatformAdminAcrossTenantsButNotDeletedRows() {
        RequestContext.getUser().setPlatformAdmin(true);
        assertEquals(105L, documents.requireReadable(4L, 105L).getId());
        assertEquals(1, documents.fetchPage(4L, null, false, null, 1, 20, null).getTotal());
        assertEquals(Set.of(9L), Set.copyOf(folders.fetchFolderList(4L, null).stream()
                .map(KnowledgeFolderEntity::getId).toList()));
        assertThrows(ApiRuntimeException.class, () -> documents.requireReadable(1L, 107L));
    }

    /** 不再向自定义模型统计传租户参数，框架仍自动过滤两边的租户与删除状态。 */
    @Test
    void shouldFilterModelReferenceQueries() {
        assertEquals(1, models.selectKnowledgeBaseCount(301L));
        assertEquals(1, connections.selectBoundKnowledgeBaseCount(201L));
        RequestContext.setTenantCode(2L);
        assertEquals(0, models.selectKnowledgeBaseCount(301L));
        assertEquals(0, connections.selectBoundKnowledgeBaseCount(201L));
    }

    /** 跨租户清理跳过租户与组织权限，但仍排除已删除队列及在用原件。 */
    @Test
    void shouldPreserveCrossTenantCleanupBoundary() {
        assertEquals(Set.of(502L, 503L), Set.copyOf(cleanups.selectDueCleanupList().stream()
                .map(StorageCleanupEntity::getId).toList()));
        assertEquals(0, cleanups.updateRetry(-1L));
        assertEquals(1, cleanups.updateRetry(502L));
    }

    /** 非空检查忽略组织可见范围，但必须排除逻辑删除项并保留租户边界。 */
    @Test
    void shouldKeepStructuralQueriesIndependentOfReadScope() {
        assertThrows(ApiRuntimeException.class, () -> folders.requireAcyclic(1L, 1L, 2L));
        assertThrows(ApiRuntimeException.class, () -> folders.removeFolder(1L, 3L));
        assertEquals(13, folders.countFolder(1L));
        RequestContext.setTenantCode(2L);
        assertEquals(0, folders.countFolder(1L));
    }

    /** 真实加载共享Druid配置，避免测试手工设置的Wall策略与生产配置偏离。 */
    private WallConfig loadSharedWallConfig() throws Exception {
        var resource = new PathMatchingResourcePatternResolver().getResource("classpath:druid.yml");
        var source = new YamlPropertySourceLoader().load("druid", resource).get(0);
        return new Binder(ConfigurationPropertySources.from(source))
                .bind("spring.datasource.druid.filter.wall.config", Bindable.of(WallConfig.class)).get();
    }

    /** 读取真实AI应用配置，验证新开关及忽略表配置的实际绑定结果。 */
    private EtdMyBatisPlusProperties loadApplicationMybatisProperties() throws Exception {
        var resource = new PathMatchingResourcePatternResolver().getResource("classpath:etd-ai-agent.yml");
        var source = new YamlPropertySourceLoader().load("ai", resource).get(0);
        return new Binder(ConfigurationPropertySources.from(source))
                .bind("etd.mybatis", Bindable.of(EtdMyBatisPlusProperties.class)).get();
    }

    /** 状态是可选业务筛选，停用记录在管理详情中仍能读取。 */
    @Test
    void shouldFilterDocumentStatusOnlyWhenRequested() {
        assertEquals(4, documents.fetchPage(1L, null, false, null, 1, 20,
                BasicConstant.DataStatus.ENABLED.getCode()).getTotal());
        var disabled = documents.fetchPage(1L, null, false, null, 1, 20,
                BasicConstant.DataStatus.DISABLED.getCode());
        assertEquals(1, disabled.getTotal());
        assertEquals(108L, disabled.getRecords().getFirst().getId());
        assertEquals(BasicConstant.DataStatus.DISABLED.getCode(), documents.requireReadable(1L, 108L).getDataStatus());
        assertEquals(109L, documents.requireReadable(3L, 109L).getId());
    }

    /** 筛选停用目录时仍返回启用的必要祖先，前端不能二次裁剪树路径。 */
    @Test
    void shouldPreserveAncestorsWhenFilteringFolderStatus() {
        var nodes = folders.fetchFolderList(1L, BasicConstant.DataStatus.DISABLED.getCode());
        assertEquals(Set.of(1L, 2L), Set.copyOf(nodes.stream().map(KnowledgeFolderEntity::getId).toList()));
        var enabled = folders.fetchFolderList(1L, BasicConstant.DataStatus.ENABLED.getCode());
        assertEquals(Set.of(4L, 5L, 6L), Set.copyOf(enabled.stream().map(KnowledgeFolderEntity::getId).toList()));
    }

    /** 三个BaseMapper管理列表与XML列表采用相同的状态筛选语义。 */
    @Test
    void shouldFilterModelAndBaseManagementLists() {
        int disabled = BasicConstant.DataStatus.DISABLED.getCode();
        int enabled = BasicConstant.DataStatus.ENABLED.getCode();
        assertEquals(2, connectionService.fetchPage(1, 20, null).getTotal());
        assertEquals(1, connectionService.fetchPage(1, 20, disabled).getTotal());
        assertEquals(1, connectionService.fetchPage(1, 20, enabled).getTotal());
        assertEquals(2, modelService.fetchPage(1, 20, null, null).getTotal());
        assertEquals(1, modelService.fetchPage(1, 20, null, disabled).getTotal());
        assertEquals(1, modelService.fetchPage(1, 20, "embedding", enabled).getTotal());
        assertEquals(4, baseService.fetchPage(1, 20, null, null).getTotal());
        assertEquals(1, baseService.fetchPage(1, 20, null, disabled).getTotal());
        assertEquals(3, baseService.fetchPage(1, 20, null, enabled).getTotal());
    }

    /** updateById自动比较并递增版本，过期版本和缺失版本都不能覆盖数据。 */
    @Test
    void shouldUseFrameworkOptimisticLockAndAuditFill() {
        withRollback(() -> {
            var update = documentPatch(103L, "新名称");
            documents.modifyDocument(update, 0L);
            var stored = documents.requireWritable(1L, 103L, false);
            assertEquals(1L, stored.getVersion());
            assertEquals("新名称", stored.getDocumentName());
            assertNull(stored.getFolderId());
            assertEquals(100L, stored.getUpdateBy());
            assertNotNull(stored.getUpdateTime());
            assertThrows(ApiRuntimeException.class, () -> documents.modifyDocument(documentPatch(103L, "覆盖"), 0L));
            assertThrows(MyBatisSystemException.class, () -> documents.modifyDocument(documentPatch(103L, "缺版本"), null));
        });
    }

    /** 普通用户仍不能跨租户写入，也不能通过公开读取获得修改权限。 */
    @Test
    void shouldKeepWriteIsolationInFramework() {
        withRollback(() -> {
            assertThrows(ApiRuntimeException.class, () -> documents.modifyDocument(documentPatch(105L, "越权"), 0L));
            assertThrows(ApiRuntimeException.class, () -> documents.modifyDocument(documentPatch(101L, "公开越权"), 0L));
            RequestContext.getUser().setPermissionTypes(Set.of());
            assertThrows(ApiRuntimeException.class, () -> documents.requireWritable(1L, 103L, false));
            assertThrows(ApiRuntimeException.class, () -> documents.modifyDocument(documentPatch(103L, "无范围"), 0L));
        });
    }

    /** 平台管理员跨租户管理现有对象无需当前租户条件，审计记录仍为操作者。 */
    @Test
    void shouldAllowPlatformAdminToModifyAcrossTenants() {
        withRollback(() -> {
            RequestContext.getUser().setPlatformAdmin(true);
            RequestContext.setTenantCode(null);
            assertEquals(5, baseService.fetchPage(1, 20, null, null).getTotal());
            documents.modifyDocument(documentPatch(105L, "全局修改"), 0L);
            assertEquals(1L, documents.requireWritable(4L, 105L, false).getVersion());
            var folder = folders.requireWritable(4L, 9L);
            folder.setFolderName("全局目录修改");
            folder.setUpdateBy(null);
            folder.setUpdateTime(null);
            folders.modifyFolder(folder, 0L);
            assertEquals(1L, folders.requireWritable(4L, 9L).getVersion());
        });
    }

    /** 模型连接凭证使用记录所属租户加密，版本由框架递增，空密钥保留语义不变。 */
    @Test
    void shouldUseRecordTenantForCredentialEncryption() {
        withRollback(() -> {
            RequestContext.getUser().setPlatformAdmin(true);
            RequestContext.setTenantCode(2L);
            when(cipher.encrypt("new-key", 1L, 201L)).thenReturn("ciphertext");
            when(cipher.requireKeyId()).thenReturn("test-key");
            var connection = connectionService.requireConnection(201L, false);
            connection.setAuthType("api_key");
            connectionService.modifyConnection(connection, "new-key", 0L);
            verify(cipher).encrypt("new-key", 1L, 201L);
            assertEquals(1L, connectionService.requireConnection(201L, false).getVersion());
        });
    }

    /** 模型、知识库和目录均使用同一乐观锁能力，目录仍支持移动回根目录。 */
    @Test
    void shouldApplyOptimisticLockToOtherManagementEntities() {
        withRollback(() -> {
            var model = modelService.requireModel(302L);
            model.setModelName("修改模型");
            modelService.modifyModel(model, 0L);
            assertEquals(1L, modelService.requireModel(302L).getVersion());
            var base = baseService.requireBase(2L, false);
            base.setKnowledgeName("修改知识库");
            baseService.modifyBase(base, 0L);
            assertEquals(1L, baseService.requireBase(2L, false).getVersion());
            jdbc.update("update ai_knowledge_folder set parent_id=4 where id=5");
            var folder = folders.requireWritable(1L, 5L);
            folder.setParentId(null);
            folders.modifyFolder(folder, 0L);
            assertEquals(1L, folders.requireWritable(1L, 5L).getVersion());
        });
    }

    /** 处理轮次是业务版本，原件替换时独立递增，并清除上轮处理快照。 */
    @Test
    void shouldKeepProcessingGenerationSeparateFromOptimisticVersion() {
        withRollback(() -> {
            populatePreviousProcessingSnapshot();
            var document = documents.requireWritable(1L, 103L, false);
            assertPreviousProcessingSnapshot(document);
            var readable = documents.requireReadable(1L, 103L);
            assertEquals(document.getProcessingOwner(), readable.getProcessingOwner());
            documents.replaceDocument(document, 0L);
            var stored = documents.requireWritable(1L, 103L, false);
            assertEquals(1L, stored.getVersion());
            assertEquals(1L, stored.getProcessingVersion());
            assertEquals(AiStage.UPLOADED, stored.getProcessingStatus());
            assertClearedProcessingSnapshot();
        });
    }

    /** 构造完整旧处理快照，覆盖实体加载后替换原件的真实路径。 */
    private void populatePreviousProcessingSnapshot() {
        // 先写入完整旧快照再读取实体，覆盖实体非空字段与Wrapper清空可能重复赋值的场景。
        jdbc.update("""
                update ai_knowledge_document
                set processing_status = 'parsing',
                    processing_owner = '4c95f9e5-bf1b-44e6-afaa-ab8d0386bf16',
                    processing_started_time = current_timestamp,
                    processing_lease_until = current_timestamp + interval '1 hour',
                    parser_version = 'old-parser', splitter_version = 'old-splitter',
                    chunk_max_tokens = 512, chunk_overlap_tokens = 64,
                    error_message = '旧处理摘要', processed_time = current_timestamp
                where id = 103
                """);
    }

    /** 校验租约、版本与处理结果均已映射到实体，避免空值造成测试误通过。 */
    private void assertPreviousProcessingSnapshot(KnowledgeDocumentEntity document) {
        assertNotNull(document.getProcessingOwner());
        assertNotNull(document.getProcessingLeaseUntil());
        assertNotNull(document.getProcessingStartedTime());
        assertEquals("old-parser", document.getParserVersion());
        assertEquals("old-splitter", document.getSplitterVersion());
        assertEquals(512, document.getChunkMaxTokens());
        assertEquals(64, document.getChunkOverlapTokens());
        assertNotNull(document.getErrorMessage());
        assertNotNull(document.getProcessedTime());
    }

    /** 直接核对数据库清空结果，防止一级缓存或内存修改掩盖更新缺失。 */
    private void assertClearedProcessingSnapshot() {
        // 直接核对数据库列，避免一级缓存或内存清空掩盖实际未更新的问题。
        var snapshot = jdbc.queryForMap("""
                select processing_owner, processing_lease_until, processing_started_time,
                       parser_version, splitter_version, chunk_max_tokens,
                       chunk_overlap_tokens, error_message, processed_time
                from ai_knowledge_document where id = 103
                """);
        for (var entry : snapshot.entrySet()) {
            assertNull(entry.getValue(), entry.getKey());
        }
    }

    /** 两个真实连接同时使用同一版本更新，只允许其中一个成功提交。 */
    @Test
    void shouldRejectOneOfTwoConcurrentWriters() throws Exception {
        jdbc.update("""
                insert into ai_knowledge_document(id,tenant_id,org_id,create_by,knowledge_base_id,document_name,
                  original_filename,storage_bucket,storage_object_key,content_type,file_size,file_sha256)
                select 199,tenant_id,org_id,create_by,knowledge_base_id,'并发测试',
                  original_filename,storage_bucket,'199',content_type,file_size,file_sha256
                from ai_knowledge_document where id=103
                """);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            var first = executor.submit(() -> updateConcurrentDocument(start, "并发一"));
            var second = executor.submit(() -> updateConcurrentDocument(start, "并发二"));
            start.countDown();
            assertNotEquals(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            assertEquals(1L, jdbc.queryForObject("select version from ai_knowledge_document where id=199", Long.class));
        } finally {
            jdbc.update("delete from ai_knowledge_document where id=199");
        }
    }

    /** 每个工作线程显式设置与清理可信身份，不依赖线程池继承请求上下文。 */
    private boolean updateConcurrentDocument(CountDownLatch start, String name) throws Exception {
        populateLoginContext();
        try {
            assertTrue(start.await(10, TimeUnit.SECONDS));
            documents.modifyDocument(documentPatch(199L, name), 0L);
            return true;
        } catch (ApiRuntimeException conflict) {
            return false;
        } finally {
            RequestContext.clean();
        }
    }

    /** 上传任务保留创建人归属校验，平台管理员可跨用户与租户操作，任务更新也受乐观锁保护。 */
    @Test
    void shouldKeepUploadOwnershipAndVersionProtection() {
        withRollback(() -> {
            jdbc.update("""
                    insert into ai_file_upload(id,tenant_id,knowledge_base_id,create_by,original_filename,file_size,
                      bucket,object_key,upload_id,state,part_size_bytes,part_count,url_expiry_seconds,concurrency,max_retries,expires_at)
                    values (601,1,1,100,'a.txt',1,'test','601','remote-601','uploading',5242880,1,300,2,2,current_timestamp+interval '1 hour'),
                           (602,1,1,200,'a.txt',1,'test','602','remote-602','uploading',5242880,1,300,2,2,current_timestamp+interval '1 hour'),
                           (603,2,4,200,'a.txt',1,'test','603','remote-603','uploading',5242880,1,300,2,2,current_timestamp+interval '1 hour')
                    """);
            assertEquals(601L, uploads.requireSession(1L, 601L, false).getId());
            assertThrows(ApiRuntimeException.class, () -> uploads.requireSession(1L, 602L, false));
            assertThrows(ApiRuntimeException.class, () -> uploads.requireSession(4L, 603L, false));
            RequestContext.getUser().setPlatformAdmin(true);
            var current = uploads.requireSession(4L, 603L, false);
            // 同一SqlSession一级缓存可能返回同一对象，显式复制成另一个请求的旧快照。
            var stale = new UploadSessionEntity();
            BeanUtils.copyProperties(current, stale);
            uploads.cancelSession(current);
            assertEquals(1L, uploads.requireSession(4L, 603L, false).getVersion());
            assertThrows(ApiRuntimeException.class, () -> uploads.completeSession(stale, 105L));
        });
    }

    /** 乐观锁装配不应破坏内置逻辑删除、审计填充或原有非空校验。 */
    @Test
    void shouldKeepNativeLogicalDeletesWorking() {
        withRollback(() -> {
            documents.removeDocument(103L);
            assertThrows(ApiRuntimeException.class, () -> documents.requireReadable(1L, 103L));
            folders.removeFolder(1L, 5L);
            assertThrows(ApiRuntimeException.class, () -> folders.requireWritable(1L, 5L));
            modelService.removeModel(302L);
            assertThrows(ApiRuntimeException.class, () -> modelService.requireModel(302L));
            baseService.removeBase(6L);
            assertThrows(ApiRuntimeException.class, () -> baseService.requireBase(6L, false));
        });
    }

    /** 写回归共享固定夹具，事务回滚不影响其他读取断言。 */
    private void withRollback(Runnable action) {
        transactions.executeWithoutResult(status -> {
            try {
                action.run();
            } finally {
                status.setRollbackOnly();
            }
        });
    }

    /** 创建只包含本次编辑字段的文档对象，不接受前端指定租户及审计属性。 */
    private KnowledgeDocumentEntity documentPatch(Long id, String name) {
        var patch = new KnowledgeDocumentEntity();
        patch.setId(id);
        patch.setDocumentName(name);
        patch.setIsPublic(false);
        return patch;
    }

    /** 关闭审计填充使用的测试Spring上下文。 */
    @AfterAll
    void closeFillContext() {
        if (fillContext != null) {
            fillContext.close();
        }
        if (dataSource != null) {
            dataSource.close();
        }
    }

    /** @return 当前范围下文档ID集合，用于结果断言 */
    private Set<Long> fetchDocumentIds() {
        return Set.copyOf(documents.fetchPage(1L, null, false, null, 1, 50, null).getRecords()
                .stream().map(KnowledgeDocumentEntity::getId).toList());
    }

    /** @return 当前可见目录及其必要导航祖先ID集合 */
    private Set<Long> fetchFolderIds() {
        return Set.copyOf(folders.fetchFolderList(1L, null).stream().map(KnowledgeFolderEntity::getId).toList());
    }

    /** 为真实服务添加生产权限切面，避免手动设置注解上下文掩盖代理问题。 */
    private <T> T createPermissionProxy(T target) {
        var proxy = new AspectJProxyFactory(target);
        proxy.addAspect(new DataPermissionAspect());
        return proxy.getProxy();
    }


}
