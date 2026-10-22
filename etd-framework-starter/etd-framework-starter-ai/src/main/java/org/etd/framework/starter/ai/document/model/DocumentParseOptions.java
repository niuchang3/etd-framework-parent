package org.etd.framework.starter.ai.document.model;

import java.time.Duration;
import java.util.List;
import org.springframework.util.Assert;
/**
 * 独立于解析引擎的公共选项；不支持的选项必须明确拒绝。
 * @param ocr 是否启用 OCR
 * @param forceOcr 是否强制覆盖原文本层；启用时必须同时开启 OCR
 * @param ocrLanguages OCR 语言列表；空列表使用引擎默认语言
 * @param tableStructure 是否识别表格结构
 * @param firstPage 起始页，从 1 开始；与结束页同时提供或同时省略
 * @param lastPage 结束页，包含该页
 * @param documentTimeout 引擎处理单份文档的超时，必须大于零
 */
public record DocumentParseOptions(
        boolean ocr,
        boolean forceOcr,
        List<String> ocrLanguages,
        boolean tableStructure,
        Integer firstPage,
        Integer lastPage,
        Duration documentTimeout) {

    /** 固定选项快照，防止并发调用时被外部集合修改。 */
    public DocumentParseOptions {
        Assert.notNull(ocrLanguages, "OCR语言列表不能为空");
        ocrLanguages = List.copyOf(ocrLanguages);
        for (String language : ocrLanguages) {
            Assert.hasText(language, "OCR语言不能为空白");
        }
        Assert.isTrue(!forceOcr || ocr, "强制OCR必须同时开启OCR");
        Assert.isTrue((firstPage == null) == (lastPage == null), "起止页码必须同时提供");
        if (firstPage != null) {
            Assert.isTrue(firstPage >= DocumentSourceLocation.FIRST_PAGE_NUMBER && lastPage >= firstPage, "页码范围无效");
        }
        Assert.notNull(documentTimeout, "文档处理超时不能为空");
        Assert.isTrue(documentTimeout.compareTo(Duration.ZERO) > 0, "文档处理超时必须大于零");
    }
}
