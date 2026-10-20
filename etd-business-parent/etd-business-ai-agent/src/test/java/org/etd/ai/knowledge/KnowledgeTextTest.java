package org.etd.ai.knowledge;

import org.etd.ai.knowledge.config.KnowledgeUploadProperties;
import org.etd.ai.knowledge.service.KnowledgeTextService;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

/** 正文解析及Token边界测试，重点覆盖中文、Emoji与解压安全限制。 */
class KnowledgeTextTest {
    /** 真实分词结果不超限，Unicode字符不会被切成替换符。 */
    @Test
    void shouldSplitUnicodeWithoutLoss() {
        var properties = new KnowledgeUploadProperties();
        var texts = new KnowledgeTextService(properties);
        String source = "这是知识库的中文资料，包含English和😀。\n".repeat(50);
        var chunks = texts.splitText(source, 40, 0);
        assertEquals(source, String.join("", chunks));
        for (String chunk : chunks) {
            assertTrue(texts.countTokens(chunk) <= 40);
            assertFalse(chunk.contains("�"));
        }
        var overlapping = texts.splitText(source, 40, 8);
        assertTrue(overlapping.size() >= chunks.size());
        assertEquals(chunks.getFirst(), overlapping.getFirst());
    }

    /** 文件扩展名不参与上传限制，但解析器对暂不支持格式明确报错。 */
    @Test
    void shouldRejectUnsupportedParsingAndOversizedText() {
        var properties = new KnowledgeUploadProperties();
        properties.setMaxTextBytes(8);
        var texts = new KnowledgeTextService(properties);
        byte[] bytes = "0123456789".getBytes(StandardCharsets.UTF_8);
        var textInput = new ByteArrayInputStream(bytes);
        assertThrows(ApiRuntimeException.class, () -> texts.parseText(textInput, "large.txt"));
        var unknownInput = new ByteArrayInputStream(bytes);
        assertThrows(ApiRuntimeException.class, () -> texts.parseText(unknownInput, "sample.exe"));
        assertThrows(ApiRuntimeException.class, () -> texts.splitText("中文", 10, 10));
    }

    /** DOCX按段落提取正文，不把XML标签作为正文。 */
    @Test
    void shouldParseDocxAndKeepParagraphs() throws Exception {
        var output = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(output)) {
            var entry = new ZipEntry("word/document.xml");
            zip.putNextEntry(entry);
            String xml = "<w:document xmlns:w='urn:word'><w:p><w:r><w:t>中文资料</w:t></w:r></w:p></w:document>";
            zip.write(xml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        var properties = new KnowledgeUploadProperties();
        var texts = new KnowledgeTextService(properties);
        var input = new ByteArrayInputStream(output.toByteArray());
        assertEquals("中文资料\n", texts.parseText(input, "资料.docx"));
    }
}
