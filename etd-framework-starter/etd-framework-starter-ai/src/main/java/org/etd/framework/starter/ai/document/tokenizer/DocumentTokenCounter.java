package org.etd.framework.starter.ai.document.tokenizer;

import com.knuddels.jtokkit.api.EncodingType;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;

/** 编辑器和正则切分共用的确定性Token计数，不生成向量、不连接模型服务。 */
public final class DocumentTokenCounter {
    /** 本地正文计数使用的明确分词器，不能冒充Docling的HuggingFace分词器。 */
    public static final String TOKENIZER = EncodingType.CL100K_BASE.getName();
    private static final JTokkitTokenCountEstimator COUNTER = new JTokkitTokenCountEstimator(EncodingType.CL100K_BASE);
    private DocumentTokenCounter() {}
    /** @param text 正文 @return cl100k_base Token数 */
    public static int count(String text) { return COUNTER.estimate(text); }
}
