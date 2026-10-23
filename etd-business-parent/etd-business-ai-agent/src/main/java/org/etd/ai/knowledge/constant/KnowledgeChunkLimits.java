package org.etd.ai.knowledge.constant;

import java.time.Duration;
import org.etd.framework.starter.ai.document.constant.DocumentOptionLimits;

/** 分片管理的输入及执行边界。 */
public final class KnowledgeChunkLimits {
    /** 单份资料最多保存的分片数。 */
    public static final int MAX_CHUNKS = DocumentOptionLimits.MAX_CHUNK_COUNT;
    /** 单片正文最大字符数，避免无界接口输入。 */
    public static final int MAX_CONTENT_CHARACTERS = 200000;
    /** 一次最多修改的分片数，与预览单页上限一致。 */
    public static final int MAX_EDIT_BATCH = 200;
    /** 一次合并最多包含的相邻分片数。 */
    public static final int MAX_MERGE_BATCH = 200;
    /** 查询远端任务的间隔。 */
    public static final long POLL_MILLIS = 500;
    /** 为提交和获取结果预留的请求时间，不替代文档处理超时。 */
    public static final Duration REQUEST_GRACE = Duration.ofSeconds(120);
    /** 合并相邻分片正文时保留段落间隔。 */
    public static final String CONTENT_SEPARATOR = "\n\n";
    private KnowledgeChunkLimits() {}
}
