package org.etd.ai.knowledge;

import java.util.List;
import java.util.ArrayList;
import com.alibaba.cloud.ai.document.DocumentWithScore;
import org.springframework.ai.document.Document;
import java.util.Map;
import java.util.Set;
import org.etd.ai.knowledge.biz.KnowledgeChunkEditBizService;
import org.etd.ai.knowledge.biz.KnowledgeRecallBizService;
import org.etd.ai.knowledge.constant.KnowledgeChunkStatus;
import org.etd.ai.knowledge.constant.KnowledgeVectorStatus;
import org.etd.ai.knowledge.converter.KnowledgeDocumentConverter;
import org.etd.ai.knowledge.biz.KnowledgeBaseBizService;
import org.etd.ai.knowledge.entity.KnowledgeBaseEntity;
import org.etd.ai.knowledge.service.KnowledgeDocumentService;
import org.etd.ai.model.service.ModelInferenceService;
import org.etd.ai.model.component.BusinessModelConnectionResolver;
import org.etd.framework.starter.ai.model.constant.ModelCapability;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.user.UserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** 在显式指定的空隔离库加载生产DDL，覆盖真实pgvector、中文全文、权限及事务发布。 */
@EnabledIfEnvironmentVariable(named = "AI_RECALL_TEST_JDBC_URL", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.profiles.active=recall-test",
        "spring.datasource.druid.url=${AI_RECALL_TEST_JDBC_URL}",
        "spring.datasource.druid.username=${AI_RECALL_TEST_DB_USER}",
        "spring.datasource.druid.password=${AI_RECALL_TEST_DB_PASSWORD}",
        "spring.datasource.druid.driver-class-name=org.postgresql.Driver",
        "etd.ai.knowledge.upload.cleanup-enabled=false",
        "etd.ai.document.docling.enabled=false",
        "management.health.redis.enabled=false",
        "logging.file=${java.io.tmpdir}/etd-recall-test"
})
class KnowledgeRecallPersistenceTest {
    private JdbcTemplate jdbc;
    @Autowired private KnowledgeChunkEditBizService editing;
    @Autowired private KnowledgeRecallBizService recall;
    @Autowired private KnowledgeDocumentService documents;
    @Autowired private KnowledgeBaseBizService baseEditing;
    @MockitoBean private ModelInferenceService inference;
    @Autowired private BusinessModelConnectionResolver connectionResolver;

    /** 只对显式指定的专用空库执行初始化脚本。 */
    @BeforeAll
    void initializeSchema() {
        var source = new DriverManagerDataSource();
        source.setUrl(System.getenv("AI_RECALL_TEST_JDBC_URL"));
        source.setUsername(System.getenv("AI_RECALL_TEST_DB_USER"));
        source.setPassword(System.getenv("AI_RECALL_TEST_DB_PASSWORD"));
        var schema = new ClassPathResource("sql/ai.sql");
        var populator = new ResourceDatabasePopulator(schema);
        populator.execute(source);
        jdbc = new JdbcTemplate(source);
    }

    /** 构造一份未启用、待确认资料；模型推理只返回确定性向量，不调用收费接口。 */
    @BeforeEach
    void prepare() {
        jdbc.execute("truncate ai_model_connection, ai_model, ai_vector_768, ai_vector_1024, ai_vector_1536, ai_knowledge_chunk, ai_knowledge_document, ai_knowledge_base");
        jdbc.execute("""
                insert into ai_model (id, provider_code, model_type, model_name, model_code, embedding)
                values (20, 'test', 'embedding', '测试向量', 'test-embedding', array[768,1024]);
                insert into ai_model_connection (id, tenant_id, model_id, protocol, base_url, auth_type)
                values (30, 900001, 20, 'openai_compatible', 'http://localhost', 'none');
                insert into ai_knowledge_base (id, tenant_id, org_id, create_by, name, retrieval_mode, embedding_connection_id, dimensions)
                values (10, 900001, 10, 100, '测试库', 'hybrid', 30, 768);
                insert into ai_knowledge_document
                    (id, knowledge_base_id, tenant_id, org_id, create_by, folder_id, document_name, original_filename,
                     file_type, storage_bucket, storage_object_key, content_type, file_size, file_sha256,
                     part_size_bytes, part_count, upload_expires_at, upload_status, uploaded_time, chunk_status, chunk_count)
                values (1, 10, 900001, 10, 100, 1, '设备指南', 'guide.md', 'md', 'test', 'test', 'text/markdown',
                        100, repeat('0',64), 10485760, 1, current_timestamp + interval '1 day',
                        'completed', current_timestamp, 'draft', 2);
                insert into ai_knowledge_chunk
                    (id, knowledge_base_id, tenant_id, org_id, document_id, chunk_index, content, token_count, source)
                values (101, 10, 900001, 10, 1, 0, '设备安装指南', 5, '{"headings":[],"pageNumbers":[],"documentItemRefs":[]}'),
                       (102, 10, 900001, 10, 1, 1, '设备维修流程', 5, '{"headings":[],"pageNumbers":[],"documentItemRefs":[]}');
                """);
        var user = new UserDetails();
        user.setId(100L);
        user.setOrgId(10L);
        user.setScopeOrgIds(Set.of(10L));
        user.setPermissionTypes(Set.of(BasicConstant.PermissionType.ORGANIZATION.getCode()));
        RequestContext.setUser(user);
        RequestContext.setTenantCode(900001L);
        when(inference.createEmbeddingList(eq(30L), any(), any())).thenAnswer(call -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            int dimensions = call.getArgument(1);
            List<String> texts = call.getArgument(2);
            if (texts.size() == 2) {
                assertThat(documents.requireDocument(1L, false).getVectorStatus()).isEqualTo(KnowledgeVectorStatus.PROCESSING);
            }
            return texts.stream().map(text -> vector(dimensions)).toList();
        });
    }

    /** 清理身份上下文，避免测试之间泄漏权限。 */
    @AfterEach
    void clearIdentity() {
        RequestContext.clean();
    }

    /** 框架字符串连接ID必须转换为Long后绑定，真实PostgreSQL不能用varchar与bigint主键比较。 */
    @Test
    void resolvesStringConnectionIdAgainstPostgreSql() {
        var configuration = connectionResolver.resolveAuthorized("900001", "30");
        assertThat(configuration.connectionId()).isEqualTo("30");
        assertThat(configuration.tenantId()).isEqualTo("900001");
        assertThat(configuration.capability()).isEqualTo(ModelCapability.EMBEDDING);
        assertThat(configuration.resolveApiKey()).isNull();
    }

    /** 确认后向量和中文全文均可测试召回，资料启停不影响召回测试。 */
    @Test
    void confirmsAndRecallsRegardlessOfPublicationStatus() {
        var state = editing.confirmChunks(1L, 0L);
        assertThat(state.getChunkStatus()).isEqualTo(KnowledgeChunkStatus.CONFIRMED);
        assertThat(state.getVectorStatus()).isEqualTo(KnowledgeVectorStatus.READY);
        var page = documents.fetchDocumentPage(1, 10, null, null, null, null, null, 10L, null);
        var listed = KnowledgeDocumentConverter.INSTANCE.toListVO(page.getRecords().getFirst());
        assertThat(listed.getVectorStatus()).isEqualTo(KnowledgeVectorStatus.READY);
        assertThat(jdbc.queryForObject("select count(*) from ai_vector_768", Integer.class)).isEqualTo(2);
        var hits = recall.testRecall(10L, "安装");
        assertThat(hits).hasSize(2);
        assertThat(hits.getFirst().getChunkId()).isEqualTo(101L);
        assertThat(hits.getFirst().getKeywordScore()).isPositive();
        assertThat(hits.getFirst().getVectorScore()).isCloseTo(1.0, within(0.000001));
        assertThat(documents.requireDocument(1L, false).getDataStatus()).isZero();
        jdbc.update("update ai_knowledge_document set data_status = 1 where id = 1");
        assertThat(recall.testRecall(10L, "安装")).hasSize(2);
    }

    /** 编辑后的草稿不能命中旧向量；重新确认使用资料当前轮次替换全部向量。 */
    @Test
    void invalidatesEditedContentAndReplacesVectors() {
        var state = editing.confirmChunks(1L, 0L);
        Map<Long, String> changes = Map.of(101L, "设备安装新规");
        state = editing.modifyChunks(1L, state.getVersion(), changes);
        assertThat(state.getVectorStatus()).isEqualTo(KnowledgeVectorStatus.STALE);
        assertThat(recall.testRecall(10L, "安装")).isEmpty();
        editing.confirmChunks(1L, state.getVersion());
        assertThat(recall.testRecall(10L, "安装")).extracting("content").contains("设备安装新规");
        assertThat(jdbc.queryForObject("select count(*) from ai_vector_768", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select min(processing_version) from ai_vector_768", Long.class)).isEqualTo(1);
    }

    /** 切换维度必须换代，旧维度向量不能混入新的向量空间。 */
    @Test
    void switchesDimensionsWithoutMixingOldSpace() {
        var state = editing.confirmChunks(1L, 0L);
        jdbc.update("update ai_knowledge_base set dimensions = 1024, version = version + 1 where id = 10");
        assertThatThrownBy(() -> recall.testRecall(10L, "安装")).hasMessageContaining("尚未建立索引");
        editing.confirmChunks(1L, state.getVersion());
        assertThat(jdbc.queryForObject("select active_index_version from ai_knowledge_base where id=10", Long.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from ai_vector_768", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from ai_vector_1024", Integer.class)).isEqualTo(2);
        assertThat(recall.testRecall(10L, "安装")).hasSize(2);
    }

    /** 模型失败以及模型调用期间发生编辑，均不发布确认或覆盖新正文。 */
    @Test
    void rejectsFailedAndStaleInference() {
        when(inference.createEmbeddingList(any(), any(), any())).thenThrow(new ApiRuntimeException("模拟模型失败"));
        assertThatThrownBy(() -> editing.confirmChunks(1L, 0L)).hasMessageContaining("模拟模型失败");
        assertThat(documents.requireDocument(1L, false).getChunkStatus()).isEqualTo(KnowledgeChunkStatus.DRAFT);
        assertThat(documents.requireDocument(1L, false).getVectorStatus()).isEqualTo(KnowledgeVectorStatus.FAILED);
        Long retryVersion = documents.requireDocument(1L, false).getVersion();
        doAnswer(call -> {
            Map<Long, String> changes = Map.of(101L, "在途人工修改");
            Long currentVersion = documents.requireDocument(1L, false).getVersion();
            editing.modifyChunks(1L, currentVersion, changes);
            var value = vector(768);
            return List.of(value, value);
        }).when(inference).createEmbeddingList(any(), any(), any());
        assertThatThrownBy(() -> editing.confirmChunks(1L, retryVersion)).hasMessageContaining("分片已变化");
        assertThat(documents.requireDocument(1L, false).getVectorStatus()).isEqualTo(KnowledgeVectorStatus.STALE);
        assertThat(jdbc.queryForObject("select count(*) from ai_vector_768", Integer.class)).isZero();
        assertThat(documents.requireDocument(1L, false).getChunkStatus()).isEqualTo(KnowledgeChunkStatus.DRAFT);
    }

    /** 共享允许读取但不能确认；管理员可以跨租户查询，普通用户不能跨租户读取。 */
    @Test
    void enforcesPermissionAndPreservesAdminAccess() {
        editing.confirmChunks(1L, 0L);
        RequestContext.getUser().setScopeOrgIds(Set.of(11L));
        assertThatThrownBy(() -> recall.testRecall(10L, "安装")).isInstanceOf(ApiRuntimeException.class);
        jdbc.update("update ai_knowledge_document set is_public = true where id = 1");
        assertThat(recall.testRecall(10L, "安装")).hasSize(2);
        assertThatThrownBy(() -> editing.confirmChunks(1L, 1L)).isInstanceOf(ApiRuntimeException.class);
        RequestContext.setTenantCode(900002L);
        assertThatThrownBy(() -> recall.testRecall(10L, "安装")).isInstanceOf(ApiRuntimeException.class);
        RequestContext.getUser().setPlatformAdmin(true);
        assertThat(recall.testRecall(10L, "安装")).hasSize(2);
    }

    /** 全文模式确认完全不调用模型，并且测试召回仍只查询已确认分片。 */
    @Test
    void supportsKeywordModeWithoutModel() {
        jdbc.update("update ai_knowledge_base set retrieval_mode = 'keyword', embedding_connection_id = null, dimensions = null where id = 10");
        assertThat(recall.testRecall(10L, "安装")).isEmpty();
        var confirmed = editing.confirmChunks(1L, 0L);
        assertThat(confirmed.getVectorStatus()).isEqualTo(KnowledgeVectorStatus.NOT_REQUIRED);
        assertThat(recall.testRecall(10L, "安装")).extracting("chunkId").containsExactly(101L);
        verifyNoInteractions(inference);
    }

    /** 向量写入中途失败时，旧确认状态与知识库代次必须和向量写入一起回滚。 */
    @Test
    void rollsBackPartialVectorWrites() {
        jdbc.execute("alter table ai_vector_768 add constraint ck_test_write_failure check (chunk_id <> 102)");
        try {
            assertThatThrownBy(() -> editing.confirmChunks(1L, 0L)).isInstanceOf(DataIntegrityViolationException.class);
            assertThat(jdbc.queryForObject("select count(*) from ai_vector_768", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("select active_index_version from ai_knowledge_base where id=10", Long.class)).isZero();
            assertThat(documents.requireDocument(1L, false).getChunkStatus()).isEqualTo(KnowledgeChunkStatus.DRAFT);
            assertThat(documents.requireDocument(1L, false).getVectorStatus()).isEqualTo(KnowledgeVectorStatus.FAILED);
        } finally {
            jdbc.execute("alter table ai_vector_768 drop constraint ck_test_write_failure");
        }
    }

    /** 重排使用分片ID关联结果并执行阈值过滤，外部调用后仍复核资料状态。 */
    @Test
    void reranksAndRechecksCurrentContent() {
        editing.confirmChunks(1L, 0L);
        jdbc.execute("""
                insert into ai_model (id, provider_code, model_type, model_name, model_code)
                values (21, 'test', 'rerank', '测试重排', 'test-rerank');
                insert into ai_model_connection (id, tenant_id, model_id, protocol, base_url, auth_type)
                values (31, 900001, 21, 'dashscope', 'http://localhost', 'none');
                update ai_knowledge_base set rerank_enabled = true, rerank_connection_id = 31,
                    rerank_threshold = 0.5 where id = 10;
                """);
        when(inference.createRerankList(eq(31L), any(), any())).thenAnswer(call -> {
            List<Document> inputs = call.getArgument(2);
            List<DocumentWithScore> results = new ArrayList<>();
            for (var input : inputs) {
                var result = new DocumentWithScore();
                result.setDocument(input);
                result.setScore(input.getId().equals("102") ? 0.9 : 0.1);
                results.add(result);
            }
            return results;
        });
        var hits = recall.testRecall(10L, "安装");
        assertThat(hits).extracting("chunkId").containsExactly(102L);
        assertThat(hits.getFirst().getRerankScore()).isEqualTo(0.9);
        doAnswer(call -> {
            jdbc.update("update ai_knowledge_document set chunk_status = 'draft' where id=1");
            return List.of();
        }).when(inference).createRerankList(eq(31L), any(), any());
        assertThat(recall.testRecall(10L, "安装")).isEmpty();
    }

    /** 验证开启滑动指针窗口扩展（contextWindowSize=1）时，召回命中结果会自动拼接相邻前后分片的正文。 */
    @Test
    void expandsContextWindowForRecallHits() {
        editing.confirmChunks(1L, 0L);
        long baseId = 10L;
        String query = "安装";
        jdbc.update("update ai_knowledge_base set retrieval_mode = 'keyword', embedding_connection_id = null, dimensions = null, context_window_size = 0 where id = 10");
        var directHits = recall.testRecall(baseId, query);
        assertThat(directHits).hasSize(1);
        assertThat(directHits.getFirst().getContent()).isEqualTo("设备安装指南");

        jdbc.update("update ai_knowledge_base set context_window_size = 1 where id = 10");
        var expandedHits = recall.testRecall(baseId, query);
        assertThat(expandedHits).hasSize(1);
        assertThat(expandedHits.getFirst().getContent()).isEqualTo("设备安装指南\n\n设备维修流程");
    }

    /** 调整模型维度会使原有文档状态失效，旧确认请求不能重新发布旧配置的向量。 */
    @Test
    void invalidatesVectorStatusAfterBaseConfigurationChanges() {
        editing.confirmChunks(1L, 0L);
        var input = createBaseSettings(1024);
        baseEditing.modifyBase(10L, input, 1L);
        assertThat(documents.requireDocument(1L, false).getVectorStatus()).isEqualTo(KnowledgeVectorStatus.STALE);
        var keyword = createBaseSettings(1024);
        keyword.setRetrievalMode("keyword");
        baseEditing.modifyBase(10L, keyword, 2L);
        assertThat(documents.requireDocument(1L, false).getVectorStatus()).isEqualTo(KnowledgeVectorStatus.NOT_REQUIRED);
    }

    /** 检索数量变更不批量失效已有向量；在途发布遇到配置版本冲突时应记录失败。 */
    @Test
    void marksFailureWhenConfigurationChangesDuringInference() {
        doAnswer(call -> {
            jdbc.update("update ai_knowledge_base set top_k = 4, version = version + 1 where id = 10");
            var value = vector(768);
            return List.of(value, value);
        }).when(inference).createEmbeddingList(any(), any(), any());
        assertThatThrownBy(() -> editing.confirmChunks(1L, 0L)).hasMessageContaining("配置已变化");
        assertThat(documents.requireDocument(1L, false).getVectorStatus()).isEqualTo(KnowledgeVectorStatus.FAILED);
    }

    /** 构造完整知识库设置以执行真实Biz校验和失效SQL。 */
    private KnowledgeBaseEntity createBaseSettings(int dimensions) {
        var input = new KnowledgeBaseEntity();
        input.setOrgId(10L);
        input.setName("测试库");
        input.setIsPublic(false);
        input.setDataStatus(BasicConstant.DataStatus.ENABLED_CODE);
        input.setRetrievalMode("hybrid");
        input.setKeywordRecallSize(30);
        input.setVectorRecallSize(30);
        input.setTopK(5);
        input.setEmbeddingConnectionId(30L);
        input.setDimensions(dimensions);
        input.setRerankEnabled(false);
        input.setContextWindowSize(0);
        return input;
    }

    /** 生成非零单位向量，让余弦结果精确可控。 */
    private float[] vector(int dimensions) {
        float[] value = new float[dimensions];
        value[0] = 1;
        return value;
    }
}
