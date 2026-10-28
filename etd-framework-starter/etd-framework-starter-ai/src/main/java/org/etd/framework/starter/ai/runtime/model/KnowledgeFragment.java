package org.etd.framework.starter.ai.runtime.model;

import java.util.Map;

/** 已授权且有序的知识片段；框架只使用文本和来源，不访问业务表。
 * @param id 全局分片标识
 * @param content 权威正文
 * @param metadata 不含凭证的引用元数据
 */
public record KnowledgeFragment(String id, String content, Map<String, Object> metadata) {
    public KnowledgeFragment {
        metadata = Map.copyOf(metadata);
    }
}
