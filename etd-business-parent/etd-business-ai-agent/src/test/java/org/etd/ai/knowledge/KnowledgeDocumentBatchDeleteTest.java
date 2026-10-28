package org.etd.ai.knowledge;

import java.util.ArrayList;
import java.util.List;
import org.etd.ai.knowledge.biz.KnowledgeDocumentBatchDeleteBizService;
import org.etd.ai.knowledge.biz.KnowledgeDocumentBizService;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;
import org.etd.ai.knowledge.service.KnowledgeChunkService;
import org.etd.ai.knowledge.service.KnowledgeDocumentService;
import org.etd.ai.knowledge.service.KnowledgeFolderService;
import org.etd.ai.knowledge.service.KnowledgeObjectService;
import org.etd.ai.knowledge.service.KnowledgeVectorService;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.etd.ai.knowledge.model.KnowledgeDocumentDeleteResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 使用真实Spring事务及隔离H2事务探针，验证逐项回滚和已有原件清理边界。 */
class KnowledgeDocumentBatchDeleteTest {
    /** 多项失败不会阻断后续项，数据库失败仅回滚当前资料，权限和归属校验先于原件清理。 */
    @Test
    void shouldCommitSuccessfulItemsAndRollbackOnlyFailedDocument() throws Exception {
        DriverManagerDataSource source = new DriverManagerDataSource("jdbc:h2:mem:batch-delete;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(source);
        jdbc.execute("create table deletion_probe (document_id bigint primary key)");
        jdbc.execute("insert into deletion_probe values (1), (2), (3), (4), (5), (6), (7)");
        KnowledgeDocumentService documents = mock(KnowledgeDocumentService.class);
        KnowledgeObjectService objects = mock(KnowledgeObjectService.class);
        KnowledgeFolderService folders = mock(KnowledgeFolderService.class);
        KnowledgeChunkService chunks = mock(KnowledgeChunkService.class);
        KnowledgeVectorService vectors = mock(KnowledgeVectorService.class);
        List<KnowledgeDocumentEntity> records = prepareDocuments(documents);
        prepareFailures(documents, objects, records, jdbc);
        KnowledgeDocumentBizService single = new KnowledgeDocumentBizService(documents, folders, objects, chunks, vectors);
        KnowledgeDocumentBizService target = transactionalProxy(single, source);
        KnowledgeDocumentBatchDeleteBizService batch = new KnowledgeDocumentBatchDeleteBizService(target);
        List<Long> ids = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L);
        KnowledgeDocumentDeleteResult result = batch.removeDocumentList(10L, ids);
        assertThat(result.successIds()).containsExactly(1L, 6L);
        assertThat(result.failures()).extracting(value -> value.id()).containsExactly(2L, 3L, 4L, 5L, 7L);
        List<Long> remaining = jdbc.queryForList("select document_id from deletion_probe order by document_id", Long.class);
        assertThat(remaining).containsExactly(2L, 3L, 4L, 5L, 7L);
        assertThat(result.failures().getLast().message()).isEqualTo("原件清理失败，请重试删除");
        verify(objects, never()).removeObject(records.get(2));
        verify(objects, never()).removeObject(records.get(4));
        verify(chunks).removeChunkList(1L);
        verify(vectors).removeVectorList(6L);
        jdbc.execute("drop table deletion_probe");
    }

    /** 资料维护失败和繁忙状态都不能进入存储清理，跨库选择亦被拒绝。 */
    private List<KnowledgeDocumentEntity> prepareDocuments(KnowledgeDocumentService documents) {
        ArrayList<KnowledgeDocumentEntity> records = new ArrayList<KnowledgeDocumentEntity>();
        for (long id = 1; id <= 7; id++) {
            KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
            document.setId(id);
            document.setKnowledgeBaseId(id == 5 ? 20L : 10L);
            records.add(document);
            when(documents.requireDocument(id, true)).thenReturn(document);
        }
        ApiRuntimeException denied = new ApiRuntimeException("资料不存在或无操作权限");
        when(documents.requireDocument(4L, true)).thenThrow(denied);
        KnowledgeDocumentEntity busy = records.get(2);
        ApiRuntimeException exception = new ApiRuntimeException("资料正在生成分片，请稍后操作");
        doThrow(exception).when(documents).requireChunkIdle(busy);
        return records;
    }

    /** 删除探针后抛出异常，必须由真实事务回滚；对象存储异常只能返回脱敏业务提示。 */
    private void prepareFailures(KnowledgeDocumentService documents, KnowledgeObjectService objects,
            List<KnowledgeDocumentEntity> records, JdbcTemplate jdbc) throws Exception {
        doAnswer(call -> {
            Long id = call.getArgument(0);
            jdbc.update("delete from deletion_probe where document_id = ?", id);
            if (id == 2L) {
                throw new ApiRuntimeException("资料已变化，请刷新后重试");
            }
            return null;
        }).when(documents).removeDocument(anyLong());
        IllegalStateException storageFailure = new IllegalStateException("internal-storage-detail");
        doThrow(storageFailure).when(objects).removeObject(records.get(6));
    }

    /** 生产方法上的REQUIRES_NEW由真实事务拦截器执行，不使用自调用模拟事务。 */
    private KnowledgeDocumentBizService transactionalProxy(KnowledgeDocumentBizService single,
            DriverManagerDataSource source) {
        DataSourceTransactionManager manager = new DataSourceTransactionManager(source);
        AnnotationTransactionAttributeSource attributes = new AnnotationTransactionAttributeSource();
        TransactionInterceptor interceptor = new TransactionInterceptor(manager, attributes);
        ProxyFactory factory = new ProxyFactory(single);
        factory.setProxyTargetClass(true);
        factory.addAdvice(interceptor);
        return (KnowledgeDocumentBizService) factory.getProxy();
    }

    /** 重复、空选择和超限批次应在执行任何资料删除前失败。 */
    @Test
    void shouldRejectInvalidSelectionBeforeAnyDeletion() {
        KnowledgeDocumentBizService single = mock(KnowledgeDocumentBizService.class);
        KnowledgeDocumentBatchDeleteBizService batch = new KnowledgeDocumentBatchDeleteBizService(single);
        List<Long> duplicate = List.of(1L, 1L);
        assertThatThrownBy(() -> batch.removeDocumentList(10L, duplicate)).isInstanceOf(ApiRuntimeException.class);
        List<Long> empty = List.of();
        assertThatThrownBy(() -> batch.removeDocumentList(10L, empty)).isInstanceOf(ApiRuntimeException.class);
        ArrayList<Long> oversized = new ArrayList<Long>();
        for (long id = 1; id <= 101; id++) {
            oversized.add(id);
        }
        assertThatThrownBy(() -> batch.removeDocumentList(10L, oversized)).isInstanceOf(ApiRuntimeException.class);
        verifyNoInteractions(single);
    }

    /** 非业务异常不向客户端泄露内部信息，后续项继续执行。 */
    @Test
    void shouldSanitizeUnexpectedFailureAndContinue() {
        KnowledgeDocumentBizService single = mock(KnowledgeDocumentBizService.class);
        IllegalStateException failure = new IllegalStateException("internal-db-detail");
        doThrow(failure).when(single).removeDocumentByBase(1L, 10L);
        KnowledgeDocumentBatchDeleteBizService batch = new KnowledgeDocumentBatchDeleteBizService(single);
        List<Long> ids = List.of(1L, 2L);
        KnowledgeDocumentDeleteResult result = batch.removeDocumentList(10L, ids);
        assertThat(result.successIds()).containsExactly(2L);
        assertThat(result.failures().getFirst().message()).isEqualTo("删除失败，请刷新后重试");
    }
}
