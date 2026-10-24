package org.etd.ai.knowledge;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;
import org.etd.ai.knowledge.constant.KnowledgeChunkStatus;
import org.etd.ai.knowledge.mapper.KnowledgeDocumentMapper;
import org.etd.ai.knowledge.service.KnowledgeDocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/** 检查真实 Lambda Wrapper 生成的筛选 SQL，防止批量与单目录条件错误叠加。 */
class KnowledgeDocumentPageQueryTest {
    private KnowledgeDocumentMapper mapper;
    private KnowledgeDocumentService service;

    /** 初始化实体字段映射，使测试执行实际 MyBatis-Plus 条件构建。 */
    @BeforeEach
    void setUp() {
        var configuration = new MybatisConfiguration();
        var assistant = new MapperBuilderAssistant(configuration, "knowledge-document-page-test");
        TableInfoHelper.initTableInfo(assistant, KnowledgeDocumentEntity.class);
        mapper = mock(KnowledgeDocumentMapper.class);
        service = new KnowledgeDocumentService(mapper);
    }

    /** 非空批量目录覆盖冲突的单目录，同时保留其他筛选、排序及分页。 */
    @ParameterizedTest
    @ValueSource(longs = {9L, 1L})
    void shouldPreferFolderIds(long folderId) {
        when(mapper.selectPage(any(), any())).thenAnswer(call -> {
            LambdaQueryWrapper<KnowledgeDocumentEntity> query = call.getArgument(1);
            String sql = query.getSqlSegment().toLowerCase();
            assertTrue(sql.contains("folder_id in"));
            assertFalse(sql.contains("folder_id ="));
            assertTrue(sql.contains("document_name like"));
            assertTrue(sql.contains("upload_status ="));
            assertTrue(sql.contains("chunk_status ="));
            assertFalse(sql.contains("processing_status"));
            assertTrue(sql.contains("order by create_time desc,id desc"));
            var parameters = query.getParamNameValuePairs().values();
            assertTrue(parameters.contains(1L));
            assertTrue(parameters.contains(2L));
            assertFalse(parameters.contains(9L));
            assertTrue(parameters.contains(KnowledgeChunkStatus.DRAFT));
            return call.getArgument(0);
        });
        List<Long> folderIds = List.of(1L, 2L);
        var page = service.fetchDocumentPage(2L, 20L, folderId, folderIds, "说明", "completed", KnowledgeChunkStatus.DRAFT);
        assertEquals(2L, page.getCurrent());
        assertEquals(20L, page.getSize());
    }

    /** 未传或传空批量目录时仍支持单目录；两者均未提供时不追加目录条件。 */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldUseSingleFolderOnlyWhenBatchIsEmpty(boolean omitted) {
        List<Long> folderIds = omitted ? null : Collections.emptyList();
        when(mapper.selectPage(any(), any())).thenAnswer(call -> {
            Wrapper<KnowledgeDocumentEntity> query = call.getArgument(1);
            String sql = query.getSqlSegment().toLowerCase();
            assertTrue(sql.contains("folder_id ="));
            assertFalse(sql.contains("folder_id in"));
            return call.getArgument(0);
        });
        service.fetchDocumentPage(1L, 10L, 9L, folderIds, null, null, null);

        doAnswer(call -> {
            Wrapper<KnowledgeDocumentEntity> query = call.getArgument(1);
            assertFalse(query.getSqlSegment().toLowerCase().contains("folder_id"));
            return call.getArgument(0);
        }).when(mapper).selectPage(any(), any());
        service.fetchDocumentPage(1L, 10L, null, folderIds, null, null, null);
    }

    /** 五种分片状态均通过绑定参数精确筛选，不在内存中过滤分页结果。 */
    @ParameterizedTest
    @ValueSource(strings = {KnowledgeChunkStatus.UNPROCESSED, KnowledgeChunkStatus.GENERATING,
            KnowledgeChunkStatus.DRAFT, KnowledgeChunkStatus.CONFIRMED, KnowledgeChunkStatus.FAILED})
    void shouldFilterByChunkStatus(String chunkStatus) {
        when(mapper.selectPage(any(), any())).thenAnswer(call -> {
            LambdaQueryWrapper<KnowledgeDocumentEntity> query = call.getArgument(1);
            assertTrue(query.getSqlSegment().contains("chunk_status ="));
            var parameters = query.getParamNameValuePairs().values();
            var expected = List.of(chunkStatus);
            var actual = List.copyOf(parameters);
            assertEquals(expected, actual);
            return call.getArgument(0);
        });
        service.fetchDocumentPage(1L, 10L, null, null, null, null, chunkStatus);
    }

    /** 未传、空字符串及空白均不限制分片状态，支持前端清空筛选。 */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void shouldOmitEmptyChunkStatus(String chunkStatus) {
        when(mapper.selectPage(any(), any())).thenAnswer(call -> {
            Wrapper<KnowledgeDocumentEntity> query = call.getArgument(1);
            assertFalse(query.getSqlSegment().contains("chunk_status"));
            return call.getArgument(0);
        });
        service.fetchDocumentPage(1L, 10L, null, null, null, null, chunkStatus);
    }
}
