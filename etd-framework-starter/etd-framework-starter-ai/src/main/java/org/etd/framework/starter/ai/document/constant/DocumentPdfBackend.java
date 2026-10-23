package org.etd.framework.starter.ai.document.constant;

import com.fasterxml.jackson.annotation.JsonValue;

/** PDF原生解析后端，协议值不依赖枚举名称。 */
public enum DocumentPdfBackend {
    /** PyPDFium2解析后端。 */
    PYPDFIUM2("pypdfium2"),
    /** 当前Docling线程化解析后端。 */
    DOCLING_PARSE("_docling_parse");

    /** Docling使用的稳定协议代码。 */
    private final String code;

    DocumentPdfBackend(String code) {
        this.code = code;
    }

    /** @return 当前选项的协议代码 */
    @JsonValue
    public String getCode() {
        return code;
    }
}
