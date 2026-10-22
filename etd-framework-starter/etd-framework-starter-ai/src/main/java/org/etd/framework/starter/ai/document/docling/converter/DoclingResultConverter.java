package org.etd.framework.starter.ai.document.docling.converter;

import ai.docling.serve.api.convert.response.InBodyConvertDocumentResponse;
import ai.docling.serve.api.chunk.response.Document;
import ai.docling.serve.api.convert.response.ErrorItem;
import ai.docling.serve.api.task.response.TaskStatus;
import ai.docling.serve.api.task.response.TaskStatusPollResponse;
import org.etd.framework.starter.ai.document.constant.DocumentParseStatus;
import org.etd.framework.starter.ai.document.constant.DocumentTaskStatus;
import org.etd.framework.starter.ai.document.docling.DoclingDocumentParser;
import org.etd.framework.starter.ai.document.exception.DocumentParseException;
import org.etd.framework.starter.ai.document.model.DocumentParseResult;
import org.etd.framework.starter.ai.document.model.DocumentParseTask;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/** SDK 对象只在适配器内部存在，公共结果通过 MapStruct 转换。 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DoclingResultConverter {
    /**
     * @param response 远端任务快照
     * @return 统一任务状态
     */
    @Mapping(target = "engine", constant = DoclingDocumentParser.ENGINE)
    @Mapping(target = "status", source = "taskStatus")
    @Mapping(target = "queuePosition", source = "taskPosition")
    DocumentParseTask toTask(TaskStatusPollResponse response);

    /**
     * @param response 单份文档结果
     * @param content 已解析的结构化内容
     * @return 不依赖 SDK 类型的结果
     */
    @Mapping(target = "markdown", source = "document.markdownContent")
    @Mapping(target = "processingSeconds", source = "processingTime")
    @Mapping(target = "warnings", source = "errors")
    @Mapping(target = "blocks", expression = "java(content.blocks())")
    @Mapping(target = "structuredDocument", expression = "java(content.document())")
    DocumentParseResult toResult(InBodyConvertDocumentResponse response, @Context DoclingContent content);

    /**
     * @param response 分块结果中附带的原文及状态
     * @param content 已提取的原文结构
     * @return 公共原文结果；分块接口不提供逐文档耗时，保留为空
     */
    @Mapping(target = "markdown", source = "content.markdownContent")
    @Mapping(target = "processingSeconds", ignore = true)
    @Mapping(target = "warnings", source = "errors")
    @Mapping(target = "blocks", expression = "java(content.blocks())")
    @Mapping(target = "structuredDocument", expression = "java(content.document())")
    DocumentParseResult toResult(Document response, @Context DoclingContent content);

    /** @param status SDK 任务状态 @return 公共执行状态 */
    default DocumentTaskStatus toTaskStatus(TaskStatus status) {
        if (status == null) {
            throw new DocumentParseException("解析引擎未返回任务状态", DocumentParseException.NO_HTTP_STATUS, null);
        }
        return switch (status) {
            case PENDING -> DocumentTaskStatus.PENDING;
            case STARTED -> DocumentTaskStatus.RUNNING;
            case SUCCESS -> DocumentTaskStatus.COMPLETED;
            case FAILURE -> DocumentTaskStatus.FAILED;
        };
    }

    /** @param status 引擎文档级状态 @return 公共文档级状态，未知状态不伪装为成功 */
    default DocumentParseStatus toParseStatus(String status) {
        for (DocumentParseStatus candidate : DocumentParseStatus.values()) {
            if (candidate.getCode().equals(status)) {
                return candidate;
            }
        }
        throw new DocumentParseException("解析引擎返回未知文档状态", DocumentParseException.NO_HTTP_STATUS, null);
    }

    /** @param error 引擎错误 @return 原始诊断信息，只供受控使用，不直接返回前端 */
    default String toWarning(ErrorItem error) {
        return error.getErrorMessage() == null ? "解析引擎未提供错误详情" : error.getErrorMessage();
    }
}
