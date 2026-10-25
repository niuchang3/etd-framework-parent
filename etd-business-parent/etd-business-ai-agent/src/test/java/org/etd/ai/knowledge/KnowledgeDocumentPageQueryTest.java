package org.etd.ai.knowledge;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;
import org.etd.ai.knowledge.mapper.KnowledgeDocumentMapper;
import org.etd.ai.knowledge.service.KnowledgeDocumentService;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
/** 知识库、目录及启停筛选必须完整交给数据库JOIN查询，不在Java中过滤分页。 */
class KnowledgeDocumentPageQueryTest {
    /** 保留全部业务筛选，由框架独立追加权限范围，SQL决定批量目录优先级。 */
    @Test
    void shouldPassKnowledgeScopeAndPublicationStatus() {
        var mapper = mock(KnowledgeDocumentMapper.class);
        var service = new KnowledgeDocumentService(mapper);
        var page = new Page<KnowledgeDocumentEntity>(2, 20);
        when(mapper.selectReadablePage(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(page);
        var folders = List.of(1L, 2L);
        var result = service.fetchDocumentPage(2, 20, 9L, folders, "说明", "completed", "draft", 100L, 0);
        assertSame(page, result);
        verify(mapper).selectReadablePage(any(), eq(100L), eq(9L), eq(folders), eq("说明"), eq("completed"),
                eq("draft"), eq(0));
    }
}
