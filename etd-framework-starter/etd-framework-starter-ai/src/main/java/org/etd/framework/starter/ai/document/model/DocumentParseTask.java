package org.etd.framework.starter.ai.document.model;

import org.etd.framework.starter.ai.document.constant.DocumentTaskStatus;
/**
 * 远程解析任务快照；任务归属与持久化由业务层维护。
 * @param engine 解析引擎代码
 * @param taskId 远程任务标识，不作为业务授权凭据
 * @param status 任务执行状态，不代表文档完整解析
 * @param queuePosition 排队位置；未提供时为空
 */
public record DocumentParseTask(
        String engine,
        String taskId,
        DocumentTaskStatus status,
        Long queuePosition) {
}
