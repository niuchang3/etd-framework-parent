package org.etd.ai.upload;

import org.etd.ai.upload.component.UploadedFileInspector;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

/** 文件限制交给客户端后，服务端仍计算真实摘要并核对传输完整性。 */
class UploadedFileInspectorTest {
    /** 未知后缀、无后缀、非UTF8文本和非Office容器均不再被服务端类型策略拦截。 */
    @ParameterizedTest
    @ValueSource(strings = {"data.archive", "no-extension", "text.txt", "office.docx", "document.pdf"})
    void shouldAcceptArbitraryContentAndCalculateDigest(String filename) throws Exception {
        var inspector = new UploadedFileInspector();
        byte[] bytes = {0, 1, -1};
        var source = new ByteArrayInputStream(bytes);
        var result = inspector.inspect(source, bytes.length, filename);
        assertEquals("26a66b061e8f48f39927c312f25293959729eee95978e2892d49d3512a5cc092", result.sha256());
        assertNotNull(result.contentType());
        if ("no-extension".equals(filename)) {
            assertEquals("application/octet-stream", result.contentType());
        }
    }

    /** 实际字节少于或多于声明值时均不登记原件，这不是业务文件大小上限。 */
    @ParameterizedTest
    @ValueSource(longs = {2, 4})
    void shouldRejectIncompleteTransfer(long expected) {
        var inspector = new UploadedFileInspector();
        byte[] bytes = {0, 1, -1};
        var source = new ByteArrayInputStream(bytes);
        assertThrows(ApiRuntimeException.class, () -> inspector.inspect(source, expected, "data.archive"));
    }
}
