package org.etd.framework.starter.ai.document.parser;

import org.etd.framework.starter.ai.document.model.DocumentParserCapabilities;

/** 文档解析引擎公共标识；具体执行方式由子接口声明，不假定所有引擎均支持远程任务。 */
public interface DocumentParser {
    /** @return 唯一引擎代码，用于选择实现和关联持久化任务 */
    String engine();

    /** @return 当前适配器实际支持的能力 */
    DocumentParserCapabilities capabilities();
}
