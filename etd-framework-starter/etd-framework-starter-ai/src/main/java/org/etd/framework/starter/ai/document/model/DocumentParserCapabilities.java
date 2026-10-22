package org.etd.framework.starter.ai.document.model;

import java.util.Set;
import org.etd.framework.starter.ai.document.constant.DocumentChunkStrategy;
/**
 * 当前适配器明确提供的解析能力，不表示所有部署都装有对应模型。
 * @param fileExtensions 允许的文件扩展名，小写且不带点
 * @param ocr 是否支持 OCR 选项
 * @param tableStructure 是否支持表格结构选项
 * @param pageRange 是否支持指定页码范围
 * @param chunkStrategies 支持的原生分块策略；不支持分块时为空集合
 */
public record DocumentParserCapabilities(
        Set<String> fileExtensions,
        boolean ocr,
        boolean tableStructure,
        boolean pageRange,
        Set<DocumentChunkStrategy> chunkStrategies) {

    /** 固定能力集合，避免注册表外部修改。 */
    public DocumentParserCapabilities {
        fileExtensions = Set.copyOf(fileExtensions);
        chunkStrategies = Set.copyOf(chunkStrategies);
    }
}
