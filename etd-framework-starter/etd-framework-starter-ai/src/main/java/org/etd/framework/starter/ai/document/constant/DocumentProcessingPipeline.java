package org.etd.framework.starter.ai.document.constant;

import com.fasterxml.jackson.annotation.JsonValue;

/** PDF与图片解析流水线，协议值不依赖枚举名称。 */
public enum DocumentProcessingPipeline {
    /** 标准版面识别。 */
    STANDARD("standard"),
    /** 提取PDF原生文本，不运行版面模型。 */
    NATIVE("native"),
    /** 视觉语言模型解析，须由服务配置模型。 */
    VLM("vlm");

    /** Docling使用的稳定协议代码。 */
    private final String code;

    DocumentProcessingPipeline(String code) {
        this.code = code;
    }

    /** @return 当前选项的协议代码 */
    @JsonValue
    public String getCode() {
        return code;
    }
}
