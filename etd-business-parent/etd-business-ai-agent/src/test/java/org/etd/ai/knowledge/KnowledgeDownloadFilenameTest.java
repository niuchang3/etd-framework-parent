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
        var client = mock(CustomMinioClient.class);
        var storage = new StorageProperties();
        var properties = new KnowledgeUploadProperties();
        var service = new KnowledgeObjectService(client, storage, properties);
        var document = new KnowledgeDocumentEntity();
        document.setStorageBucket("knowledge-test");
        document.setStorageObjectKey("knowledge/1/original");
        document.setOriginalFilename(filename);
        document.setDocumentName("已修改的资料显示名称");
        service.generateDownloadUrl(document);
        var arguments = ArgumentCaptor.forClass(GetPresignedObjectUrlArgs.class);
        verify(client).getPresignedObjectUrl(arguments.capture());
        var signed = arguments.getValue();
        assertEquals(Method.GET, signed.method());
        assertEquals("knowledge/1/original", signed.object());
        var dispositions = signed.extraQueryParams().get("response-content-disposition");
        assertEquals(1, dispositions.size());
        var disposition = ContentDisposition.parse(dispositions.iterator().next());
        assertTrue(disposition.isAttachment());
        assertEquals(filename, disposition.getFilename());
    }
}
