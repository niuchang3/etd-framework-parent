package org.etd.framework.starter.ai.document.parser;

import org.etd.framework.starter.ai.document.exception.DocumentParseException;
import org.etd.framework.starter.ai.document.model.DocumentParseRequest;
import org.etd.framework.starter.ai.document.model.DocumentParseTask;
import org.etd.framework.starter.ai.document.model.DocumentParseResult;

/** 远程异步解析契约；不启动轮询、不持久化任务、不自动重提失败请求。 */
public interface AsynchronousDocumentParser extends DocumentParser {
    /**
     * 提交单份文档；超时可能已在远端创建任务，调用方不得盲目重试。
     * @param request 已完成访问授权的文件与选项
     * @return 远端任务快照
     * @throws IllegalArgumentException 输入或选项不符合要求
     * @throws DocumentParseException 文件读取、远端调用或响应异常
     */
    DocumentParseTask submit(DocumentParseRequest request);

    /**
     * 执行一次状态查询，任务归属由调用方校验。
     * @param taskId 当前引擎的任务标识
     * @return 当前执行状态
     * @throws IllegalArgumentException 标识格式无效
     * @throws DocumentParseException 远端调用或响应异常
     */
    DocumentParseTask fetchStatus(String taskId);

    /**
     * 获取已完成任务的结果，不等待任务完成，调用方必须检查文档级状态。
     * @param taskId 当前引擎的任务标识
     * @return Markdown 与结构化文档
     * @throws IllegalArgumentException 标识格式无效
     * @throws DocumentParseException 任务未就绪、远端调用或响应异常
     */
    DocumentParseResult fetchResult(String taskId);
}
