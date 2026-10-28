package org.etd.ai.knowledge;

import io.minio.CustomMinioClient;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;
import org.etd.ai.knowledge.config.KnowledgeUploadProperties;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;
import org.etd.ai.knowledge.service.KnowledgeObjectService;
import org.etd.framework.starter.storage.properties.StorageProperties;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ContentDisposition;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** 验证下载签名使用原始文件名，避免浏览器保存为对象键末段original。 */
class KnowledgeDownloadFilenameTest {
    /** 原始名称及后缀进入签名响应头，中文、空格、引号及加号均可正确解码。 */
    @ParameterizedTest
    @ValueSource(strings = {"report.pdf", "知识资料.md", "季度 报表+附件.xlsx", "报告\"终稿\".docx"})
    void shouldIncludeOriginalFilenameInSignedResponseHeaders(String filename) throws Exception {
        CustomMinioClient client = mock(CustomMinioClient.class);
        StorageProperties storage = new StorageProperties();
        KnowledgeUploadProperties properties = new KnowledgeUploadProperties();
        KnowledgeObjectService service = new KnowledgeObjectService(client, storage, properties);
        KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
        document.setStorageBucket("knowledge-test");
        document.setStorageObjectKey("knowledge/1/original");
        document.setOriginalFilename(filename);
        document.setDocumentName("已修改的资料显示名称");
        service.generateDownloadUrl(document);
        ArgumentCaptor<GetPresignedObjectUrlArgs> arguments = ArgumentCaptor.forClass(GetPresignedObjectUrlArgs.class);
        verify(client).getPresignedObjectUrl(arguments.capture());
        GetPresignedObjectUrlArgs signed = arguments.getValue();
        assertEquals(Method.GET, signed.method());
        assertEquals("knowledge/1/original", signed.object());
        Collection<String> dispositions = signed.extraQueryParams().get("response-content-disposition");
        assertEquals(1, dispositions.size());
        ContentDisposition disposition = ContentDisposition.parse(dispositions.iterator().next());
        assertTrue(disposition.isAttachment());
        assertEquals(filename, disposition.getFilename());
    }
}
