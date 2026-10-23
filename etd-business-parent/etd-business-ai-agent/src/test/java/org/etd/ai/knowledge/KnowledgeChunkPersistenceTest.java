package org.etd.ai.knowledge;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import org.etd.ai.knowledge.biz.*;
import org.etd.ai.knowledge.constant.KnowledgeChunkStatus;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;
import org.etd.ai.knowledge.service.*;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.ai.document.constant.DocumentChunkStrategy;
import org.etd.framework.starter.ai.document.constant.RegexBoundaryPlacement;
import org.etd.framework.starter.ai.document.model.*;
import org.etd.framework.starter.ai.document.parser.DocumentParserRegistry;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import javax.sql.DataSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verifyNoInteractions;
import org.etd.ai.knowledge.converter.KnowledgeDocumentConverter;
import org.etd.ai.knowledge.converter.KnowledgeChunkConverter;
import org.etd.ai.knowledge.converter.KnowledgeFolderConverter;

/** 仅在显式隔离PostgreSQL库中执行生产DDL和真实事务，不触碰已有开发资料。 */
@EnabledIfEnvironmentVariable(named = "AI_CHUNK_TEST_JDBC_URL", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.profiles.active=chunk-test",
        "spring.datasource.druid.url=${AI_CHUNK_TEST_JDBC_URL}",
        "spring.datasource.druid.username=${AI_CHUNK_TEST_DB_USER}",
        "spring.datasource.druid.password=${AI_CHUNK_TEST_DB_PASSWORD}",
        "spring.datasource.druid.driver-class-name=org.postgresql.Driver",
        "etd.ai.knowledge.upload.cleanup-enabled=false",
        "etd.ai.document.docling.enabled=true",
        "etd.ai.document.docling.base-url=${AI_CHUNK_TEST_DOCLING_URL}",
        "management.health.redis.enabled=false",
        "logging.file=${java.io.tmpdir}/etd-chunk-test"
})
class KnowledgeChunkPersistenceTest {
    @Autowired private DataSource dataSource;
    /** 初始化脚本由部署连接执行，业务读写仍经过真实Druid过滤链。 */
    private JdbcTemplate setupJdbc;
    @Autowired private KnowledgeChunkGenerationBizService generation;
    @Autowired private KnowledgeChunkEditBizService editing;
    @Autowired private KnowledgeDocumentService documents;
    @Autowired private KnowledgeDocumentBizService documentEditing;
    @Autowired private KnowledgeUploadBizService uploads;
    @Autowired private KnowledgeFolderService folders;
    @Autowired private KnowledgeChunkService chunks;
    @Autowired private DocumentParserRegistry registry;
    @MockitoBean private KnowledgeChunkGenerationService engine;
    @MockitoBean private KnowledgeObjectService objects;

    /** 在外部新建的空测试库内完整执行生产初始化定义。 */
    @BeforeAll
    void createSchema() {
        var resource = new ClassPathResource("sql/ai.sql");
        var setup = new DriverManagerDataSource();
        setup.setUrl(System.getenv("AI_CHUNK_TEST_JDBC_URL"));
        setup.setUsername(System.getenv("AI_CHUNK_TEST_DB_USER"));
        setup.setPassword(System.getenv("AI_CHUNK_TEST_DB_PASSWORD"));
        var populator = new ResourceDatabasePopulator(resource);
        populator.execute(setup);
        setupJdbc = new JdbcTemplate(setup);
    }

    /** 每例重建两份独立资料，提供固定原件及引擎结果；不使用真实业务桶。 */
    @BeforeEach
    void prepare() throws Exception {
        setupJdbc.execute("truncate table ai_knowledge_chunk, ai_knowledge_document, ai_knowledge_folder");
        setupJdbc.execute("""
                insert into ai_knowledge_folder (id, tenant_id, org_id, create_by, parent_id, folder_name)
                values (4, 900001, 10, 100, null, '共享祖先'),
                       (1, 900001, 10, 100, 4, '资料目录'),
                       (3, 900001, 10, 100, null, '非共享目录')
                """);
        var user = new UserDetails();
        user.setId(100L);
        user.setOrgId(10L);
        var permissionTypes = Set.of(BasicConstant.PermissionType.ORGANIZATION.getCode());
        user.setPermissionTypes(permissionTypes);
        user.setScopeOrgIds(Set.of(10L));
        RequestContext.setUser(user);
        RequestContext.setTenantCode(900001L);
        insertDocument(1L);
        insertDocument(2L);
        setupJdbc.update("update ai_knowledge_document set folder_id = 3 where id = 2");
        byte[] original = ("# One\n\n" + "Document content and chunk editing. ".repeat(80)).getBytes(StandardCharsets.UTF_8);
        when(objects.openObject(any())).thenAnswer(call -> new ByteArrayInputStream(original));
        List<DocumentChunk> result = new ArrayList<>();
        for (int index = 0; index < 3; index++) {
            var headings = List.of("标题" + index);
            var pages = List.of(index + 1);
            var refs = List.of("#/texts/" + index);
            List<String> captions = List.of();
            Map<String, Object> metadata = Map.of();
            var chunk = new DocumentChunk("sample.md", index, "正文" + index, "正文" + index,
                    3, headings, captions, refs, pages, metadata);
            result.add(chunk);
        }
        when(engine.generateChunkList(any(), any(), any())).thenReturn(result);
    }

    /** 释放线程上的可信身份，避免跨测试复用权限。 */
    @AfterEach
    void cleanIdentity() { RequestContext.clean(); }

    /** JSON参数和来源落库后能够恢复，编辑、合并、确认形成完整事务链路。 */
    @Test
    void persistsEditsMergesAndConfirms() {
        var parameters = parameters(DocumentChunkStrategy.HYBRID);
        var state = generation.generateChunks(1L, 0L, parameters);
        assertThat(state.getChunkStatus()).isEqualTo(KnowledgeChunkStatus.DRAFT);
        var reloaded = documents.requireDocument(1L, false);
        assertThat(reloaded.getChunkParameters()).isEqualTo(parameters);
        var records = chunks.fetchChunkList(1L);
        assertThat(records).hasSize(3);
        Map<Long, String> changes = Map.of(records.getFirst().getId(), "人工修改正文");
        state = editing.modifyChunks(1L, state.getVersion(), changes);
        var ids = List.of(records.get(0).getId(), records.get(1).getId());
        state = editing.mergeChunks(1L, state.getVersion(), ids);
        var merged = chunks.fetchChunkList(1L);
        assertThat(merged).hasSize(2);
        assertThat(merged.getFirst().getContent()).isEqualTo("人工修改正文\n\n正文1");
        assertThat(merged.getFirst().getSource().pageNumbers()).containsExactly(1, 2);
        state = editing.confirmChunks(1L, state.getVersion());
        assertThat(state.getChunkStatus()).isEqualTo(KnowledgeChunkStatus.CONFIRMED);
        assertThat(state.getChunkConfirmedTime()).isNotNull();
        state = editing.modifyChunks(1L, state.getVersion(), changes);
        assertThat(state.getChunkStatus()).isEqualTo(KnowledgeChunkStatus.DRAFT);
        assertThat(documents.requireDocument(1L, false).getChunkConfirmedTime()).isNull();
    }

    /** 重新生成失败不删除人工内容，也不覆盖最近成功参数。 */
    @Test
    void preservesOldResultsWhenRegenerationFails() {
        var parameters = parameters(DocumentChunkStrategy.HYBRID);
        var state = generation.generateChunks(1L, 0L, parameters);
        var before = chunks.fetchChunkList(1L);
        when(engine.generateChunkList(any(), any(), any())).thenThrow(new ApiRuntimeException("测试引擎失败"));
        state = generation.generateChunks(1L, state.getVersion(), parameters);
        assertThat(state.getChunkStatus()).isEqualTo(KnowledgeChunkStatus.FAILED);
        assertThat(state.getChunkCount()).isEqualTo(3);
        assertThat(chunks.fetchChunkList(1L)).extracting("id").containsExactlyElementsOf(before.stream().map(c -> c.getId()).toList());
        assertThat(documents.requireDocument(1L, false).getChunkParameters()).isEqualTo(parameters);
    }

    /** 非相邻、跨资料及过期版本均被拒绝；批量编辑失败会回滚前面已更新的正文。 */
    @Test
    void rejectsStaleVersionsAndRollsBackInvalidBatches() {
        var parameters = parameters(DocumentChunkStrategy.HYBRID);
        var state = generation.generateChunks(1L, 0L, parameters);
        var records = chunks.fetchChunkList(1L);
        var nonAdjacent = List.of(records.get(0).getId(), records.get(2).getId());
        Long version = state.getVersion();
        assertThatThrownBy(() -> editing.mergeChunks(1L, version, nonAdjacent)).isInstanceOf(ApiRuntimeException.class);
        generation.generateChunks(2L, 0L, parameters);
        Long foreignId = chunks.fetchChunkList(2L).getFirst().getId();
        Map<Long, String> batch = new LinkedHashMap<>();
        batch.put(records.getFirst().getId(), "应被回滚的修改");
        batch.put(foreignId, "跨资料修改");
        assertThatThrownBy(() -> editing.modifyChunks(1L, version, batch)).isInstanceOf(ApiRuntimeException.class);
        assertThat(chunks.fetchChunkList(1L).getFirst().getContent()).isEqualTo("正文0");
        Map<Long, String> edit = Map.of(records.getFirst().getId(), "新正文");
        editing.modifyChunks(1L, version, edit);
        assertThatThrownBy(() -> editing.modifyChunks(1L, version, edit)).isInstanceOf(ApiRuntimeException.class);
    }

    /** 生成请求超时后用户编辑使其失效，旧请求随后失败也不能覆盖人工保存状态。 */
    @Test
    void expiredGenerationCannotOverwriteNewUserEdits() {
        var parameters = parameters(DocumentChunkStrategy.HYBRID);
        var initial = generation.generateChunks(1L, 0L, parameters);
        Long firstId = chunks.fetchChunkList(1L).getFirst().getId();
        when(engine.generateChunkList(any(), any(), any())).thenAnswer(call -> {
            setupJdbc.update("update ai_knowledge_document set chunk_generation_expires_at = current_timestamp - interval '1 second' where id = 1");
            var current = documents.requireDocument(1L, false);
            Map<Long, String> edits = Map.of(firstId, "超时后人工保存");
            editing.modifyChunks(1L, current.getVersion(), edits);
            throw new ApiRuntimeException("模拟旧请求随后失败");
        });
        Long version = initial.getVersion();
        assertThatThrownBy(() -> generation.generateChunks(1L, version, parameters))
                .isInstanceOf(ApiRuntimeException.class).hasMessageContaining("已失效");
        var result = documents.requireDocument(1L, false);
        assertThat(result.getChunkStatus()).isEqualTo(KnowledgeChunkStatus.DRAFT);
        assertThat(chunks.fetchChunkList(1L).getFirst().getContent()).isEqualTo("超时后人工保存");
    }

    /** 分片与资料继承相同的组织和租户权限，平台管理员可跨范围访问。 */
    @Test
    void isolatesChunkAccessAndKeepsPlatformAdminAccess() {
        var parameters = parameters(DocumentChunkStrategy.HYBRID);
        generation.generateChunks(1L, 0L, parameters);
        RequestContext.getUser().setScopeOrgIds(Set.of(11L));
        assertThat(chunks.fetchChunkList(1L)).isEmpty();
        assertThatThrownBy(() -> documents.requireDocument(1L, false)).isInstanceOf(ApiRuntimeException.class);
        RequestContext.getUser().setScopeOrgIds(Set.of(10L));
        RequestContext.setTenantCode(900002L);
        assertThat(chunks.fetchChunkList(1L)).isEmpty();
        RequestContext.getUser().setPlatformAdmin(true);
        assertThat(chunks.fetchChunkList(1L)).hasSize(3);
        assertThat(documents.requireDocument(1L, false)).isNotNull();
    }

    /** 共享仅扩展同租户读取；维护前置检查保护生成、对象存储和批量编辑的副作用。 */
    @Test
    void sharesDocumentsAndChunksWithoutGrantingWriteAccess() {
        var parameters = parameters(DocumentChunkStrategy.HYBRID);
        var initial = generation.generateChunks(1L, 0L, parameters);
        assertThat(initial.getIsPublic()).isFalse();
        assertThat(chunks.fetchChunkList(1L)).allSatisfy(chunk -> assertThat(chunk.getIsPublic()).isFalse());
        var input = publicConfiguration(true);
        documentEditing.modifyDocument(1L, input, initial.getVersion());
        var shared = documents.requireDocument(1L, false);
        var chunkIds = chunks.fetchChunkList(1L).stream().map(chunk -> chunk.getId()).toList();
        RequestContext.getUser().setId(200L);
        RequestContext.getUser().setScopeOrgIds(Set.of(11L));
        assertThat(documents.fetchDocumentPage(1L, 20L, null, null, null, null).getRecords())
                .extracting("id").containsExactly(1L);
        assertThat(KnowledgeDocumentConverter.INSTANCE.toVO(documents.requireDocument(1L, false)).getWritable()).isFalse();
        assertThat(chunks.fetchChunkList(1L)).hasSize(3)
                .allSatisfy(chunk -> assertThat(KnowledgeChunkConverter.INSTANCE.toVO(chunk).getWritable()).isFalse());
        assertThat(folders.fetchDocumentFolderList(null, null)).extracting("id").containsExactly(1L, 4L);
        assertThat(folders.fetchDocumentFolderList(null, null)).allSatisfy(folder ->
                assertThat(KnowledgeFolderConverter.INSTANCE.toVO(folder).getNavigationOnly()).isTrue());
        assertThat(folders.fetchFolderList(null, null)).isEmpty();
        RequestContext.getUser().setPermissionTypes(Set.of());
        assertThat(folders.fetchDocumentFolderList(null, null)).extracting("id").containsExactly(1L, 4L);
        assertThat(documents.requireDocument(1L, false)).isNotNull();
        assertThat(chunks.fetchChunkList(1L)).hasSize(3);
        assertThatThrownBy(() -> documents.requireDocument(2L, false)).isInstanceOf(ApiRuntimeException.class);
        clearInvocations(objects, engine);
        Long version = shared.getVersion();
        Map<Long, String> edits = Map.of(chunkIds.getFirst(), "不允许覆盖共享正文");
        assertThatThrownBy(() -> editing.modifyChunks(1L, version, edits)).isInstanceOf(ApiRuntimeException.class);
        assertThatThrownBy(() -> editing.mergeChunks(1L, version, chunkIds)).isInstanceOf(ApiRuntimeException.class);
        assertThatThrownBy(() -> editing.confirmChunks(1L, version)).isInstanceOf(ApiRuntimeException.class);
        assertThatThrownBy(() -> generation.generateChunks(1L, version, parameters)).isInstanceOf(ApiRuntimeException.class);
        assertThatThrownBy(() -> documentEditing.modifyDocument(1L, input, version)).isInstanceOf(ApiRuntimeException.class);
        assertThatThrownBy(() -> documentEditing.removeDocument(1L)).isInstanceOf(ApiRuntimeException.class);
        assertThatThrownBy(() -> uploads.createPartUrl(1L, 1)).isInstanceOf(ApiRuntimeException.class);
        verifyNoInteractions(objects, engine);
        RequestContext.setTenantCode(900002L);
        assertThatThrownBy(() -> documents.requireDocument(1L, false)).isInstanceOf(ApiRuntimeException.class);
        assertThat(chunks.fetchChunkList(1L)).isEmpty();
        assertThat(folders.fetchDocumentFolderList(null, null)).isEmpty();
    }

    /** 撤回共享与分片标记同事务更新，已保存结果不被重新生成或删除。 */
    @Test
    void retractsSharingAndPreservesContent() {
        var parameters = parameters(DocumentChunkStrategy.HYBRID);
        var initial = generation.generateChunks(1L, 0L, parameters);
        var sharedInput = publicConfiguration(true);
        documentEditing.modifyDocument(1L, sharedInput, initial.getVersion());
        var current = documents.requireDocument(1L, false);
        var privateInput = publicConfiguration(false);
        documentEditing.modifyDocument(1L, privateInput, current.getVersion());
        assertThat(chunks.fetchChunkList(1L)).hasSize(3)
                .allSatisfy(chunk -> assertThat(chunk.getIsPublic()).isFalse());
        RequestContext.getUser().setScopeOrgIds(Set.of(11L));
        assertThatThrownBy(() -> documents.requireDocument(1L, false)).isInstanceOf(ApiRuntimeException.class);
        assertThat(chunks.fetchChunkList(1L)).isEmpty();
        assertThat(folders.fetchDocumentFolderList(null, null)).isEmpty();
    }

    /** 由其他部门管理者生成时，分片继承资料创建人，保证原作者本人范围始终一致。 */
    @Test
    void inheritsDocumentCreatorAndPublicFlagOnRegeneration() {
        var input = publicConfiguration(true);
        documentEditing.modifyDocument(1L, input, 0L);
        RequestContext.getUser().setId(200L);
        var current = documents.requireDocument(1L, false);
        var parameters = parameters(DocumentChunkStrategy.HYBRID);
        generation.generateChunks(1L, current.getVersion(), parameters);
        RequestContext.getUser().setId(100L);
        RequestContext.getUser().setPermissionTypes(Set.of(BasicConstant.PermissionType.SELF.getCode()));
        var records = chunks.fetchChunkList(1L);
        assertThat(records).hasSize(3).allSatisfy(chunk -> {
            assertThat(chunk.getCreateBy()).isEqualTo(100L);
            assertThat(chunk.getIsPublic()).isTrue();
        });
        var privateInput = publicConfiguration(false);
        current = documents.requireDocument(1L, false);
        documentEditing.modifyDocument(1L, privateInput, current.getVersion());
        assertThat(chunks.fetchChunkList(1L)).hasSize(3)
                .allSatisfy(chunk -> assertThat(chunk.getIsPublic()).isFalse());
    }

    private KnowledgeDocumentEntity publicConfiguration(boolean shared) {
        var input = new KnowledgeDocumentEntity();
        input.setFolderId(1L);
        input.setDocumentName("共享资料");
        input.setIsPublic(shared);
        return input;
    }

    /** 使用真实Docling服务、真实PostgreSQL和实际参数转换，覆盖原生与正则两条生成路径。 */
    @ParameterizedTest
    @EnumSource(value = DocumentChunkStrategy.class, names = {"HYBRID", "REGEX"})
    void generatesThroughRealDocling(DocumentChunkStrategy strategy) {
        var actual = new KnowledgeChunkGenerationService(registry);
        when(engine.generateChunkList(any(), any(), any())).thenAnswer(call ->
                actual.generateChunkList(call.getArgument(0), call.getArgument(1), call.getArgument(2)));
        var parameters = parameters(strategy);
        var state = generation.generateChunks(1L, 0L, parameters);
        assertThat(state.getChunkStatus()).isEqualTo(KnowledgeChunkStatus.DRAFT);
        assertThat(chunks.fetchChunkList(1L)).isNotEmpty();
        assertThat(chunks.fetchChunkList(1L).getFirst().getSource().documentItemRefs()).isNotEmpty();
    }

    private void insertDocument(long id) {
        String sql = """
                insert into ai_knowledge_document
                    (id, tenant_id, org_id, create_by, folder_id, document_name, original_filename, file_type,
                     storage_bucket, storage_object_key, content_type, file_size, file_sha256,
                     part_size_bytes, part_count, upload_expires_at, upload_status, uploaded_time)
                values (?, 900001, 10, 100, 1, '测试资料', 'sample.md', 'md', 'test', 'test', 'text/markdown',
                        100, ?, 10485760, 1, current_timestamp + interval '1 day', 'completed', current_timestamp)
                """;
        setupJdbc.update(sql, id, "0".repeat(64));
    }
    private DocumentChunkParameters parameters(DocumentChunkStrategy strategy) {
        var basic = DocumentChunkParameters.Basic.builder().strategy(strategy).tableStructure(true)
                .useMarkdownTables(true).maxTokens(64);
        var normal = DocumentChunkParameters.Normal.builder();
        if (strategy == DocumentChunkStrategy.REGEX) {
            basic.pattern("(?m)^# ");
            normal.boundaryPlacement(RegexBoundaryPlacement.NEXT);
        }
        var timeout = Duration.ofSeconds(90);
        var advanced = DocumentChunkParameters.Advanced.builder().documentTimeout(timeout).build();
        return new DocumentChunkParameters(basic.build(), normal.build(), advanced);
    }
}
