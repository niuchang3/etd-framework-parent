package org.etd.ai.knowledge;

import org.etd.ai.knowledge.biz.KnowledgeDocumentBizService;
import org.etd.ai.knowledge.constant.KnowledgeChunkStatus;
import org.etd.ai.knowledge.constant.KnowledgePolicy;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;
import org.etd.ai.knowledge.service.KnowledgeChunkService;
import org.etd.ai.knowledge.service.KnowledgeDocumentService;
import org.etd.ai.knowledge.service.KnowledgeFolderService;
import org.etd.ai.knowledge.service.KnowledgeObjectService;
import org.etd.ai.knowledge.service.KnowledgeVectorService;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** 文档启用仍须检查上传和分片确认，全文模式不依赖向量索引。 */
class KnowledgeDocumentStatusTest {
    /** 确认内容可启用，未确认内容仍然被拒绝。 */
    @Test
    void shouldEnableConfirmedDocumentWithoutVectorInitialization() {
        KnowledgeDocumentService documents = mock(KnowledgeDocumentService.class);
        KnowledgeFolderService folders = mock(KnowledgeFolderService.class);
        KnowledgeObjectService objects = mock(KnowledgeObjectService.class);
        KnowledgeChunkService chunks = mock(KnowledgeChunkService.class);
        KnowledgeVectorService vectors = mock(KnowledgeVectorService.class);
        KnowledgeDocumentBizService biz = new KnowledgeDocumentBizService(documents, folders, objects, chunks, vectors);
        KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
        document.setVersion(0L);
        document.setChunkStatus(KnowledgeChunkStatus.CONFIRMED);
        document.setChunkCount(1);
        document.setUploadStatus(KnowledgePolicy.COMPLETED);
        when(documents.requireDocument(1L, true)).thenReturn(document);
        biz.modifyDocumentStatus(1L, 0L, BasicConstant.DataStatus.ENABLED_CODE);
        assertThat(document.getDataStatus()).isEqualTo(BasicConstant.DataStatus.ENABLED_CODE);
        verify(documents).modifyDocument(document);
        document.setChunkStatus(KnowledgeChunkStatus.DRAFT);
        assertThatThrownBy(() -> biz.modifyDocumentStatus(1L, 0L, BasicConstant.DataStatus.ENABLED_CODE))
                .isInstanceOf(ApiRuntimeException.class).hasMessageContaining("确认有效分片");
    }
}
