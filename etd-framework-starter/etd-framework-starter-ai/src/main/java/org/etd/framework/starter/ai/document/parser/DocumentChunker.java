package org.etd.framework.starter.ai.document.parser;

import org.etd.framework.starter.ai.document.exception.DocumentParseException;
import org.etd.framework.starter.ai.document.model.DocumentChunkRequest;
import org.etd.framework.starter.ai.document.model.DocumentChunkResult;
import org.etd.framework.starter.ai.document.model.DocumentParseTask;

/** 原生文档分块能力；由引擎执行，框架不自行实现 Token 切分算法，不创建后台轮询。 */
public interface DocumentChunker extends DocumentParser {
    /**
     * 同步解析并分块，返回前会等待远端处理；大文件宜使用异步任务。
     * @param request 已授权文档和当前引擎支持的分块选项
     * @return 分块结果及按需附带的原文结果
     * @throws IllegalArgumentException 输入或选项无效
     * @throws DocumentParseException 文件读取、远端调用或结果转换失败
     */
    DocumentChunkResult chunk(DocumentChunkRequest request);

    /**
     * 提交解析与分块任务；超时可能已创建任务，不自动重提。
     * @param request 已授权文档和当前引擎支持的分块选项
     * @return 远端任务快照，调用方持久化并维护归属
     * @throws IllegalArgumentException 输入或选项无效
     * @throws DocumentParseException 文件读取或远端调用失败
     */
    DocumentParseTask submitChunk(DocumentChunkRequest request);

    /**
     * 单次查询任务状态，与普通解析任务共享同一查询语义。
     * @param taskId 当前引擎的分块任务标识，调用方已校验归属
     * @return 任务执行状态，不保证文档完整成功
     * @throws IllegalArgumentException 任务标识无效
     * @throws DocumentParseException 远端调用或响应异常
     */
    DocumentParseTask fetchStatus(String taskId);

    /**
     * 获取已完成分块任务的结果，不自动等待。
     * @param taskId 当前引擎的分块任务标识，不能传入普通解析任务标识
     * @return 分块结果及按需附带的原文结果
     * @throws IllegalArgumentException 任务标识无效
     * @throws DocumentParseException 结果未就绪、任务类型不匹配或远端调用失败
     */
    DocumentChunkResult fetchChunkResult(String taskId);
}
