package org.etd.framework.starter.ai.document.parser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.Assert;

/** 不可变的引擎注册表；显式选择引擎，不自动切换或降级。 */
public final class DocumentParserRegistry {
    private final Map<String, DocumentParser> parsers;

    /**
     * @param parserList 已注册的解析引擎
     * @throws IllegalArgumentException 引擎为空、代码为空或重复
     */
    public DocumentParserRegistry(List<DocumentParser> parserList) {
        Assert.notNull(parserList, "解析引擎列表不能为空");
        Map<String, DocumentParser> registered = new LinkedHashMap<>();
        for (DocumentParser parser : parserList) {
            Assert.notNull(parser, "解析引擎不能为空");
            Assert.hasText(parser.engine(), "解析引擎代码不能为空");
            Assert.isTrue(registered.putIfAbsent(parser.engine(), parser) == null, "解析引擎代码重复");
        }
        this.parsers = Map.copyOf(registered);
    }

    /**
     * @param engine 引擎代码
     * @return 对应解析器，调用方按其执行接口使用
     * @throws IllegalArgumentException 引擎未注册
     */
    public DocumentParser require(String engine) {
        Assert.hasText(engine, "解析引擎代码不能为空");
        DocumentParser parser = parsers.get(engine);
        Assert.notNull(parser, "指定文档解析引擎未注册");
        return parser;
    }
}
