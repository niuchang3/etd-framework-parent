package org.etd.ai.knowledge.controller;

import org.etd.common.permission.constant.KnowledgePermissionCode;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.etd.framework.starter.client.core.permission.annotation.Permission;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.biz.KnowledgeProcessingBizService;
import org.etd.ai.knowledge.controller.vo.*;
import org.etd.ai.knowledge.converter.*;
import org.etd.ai.knowledge.service.*;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 资料处理入口，解析与确认为显式操作，不把上传成功等同于可检索。 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/v1/knowledge-documents/{id}")
@Permission(KnowledgePermissionCode.DOCUMENT)
public class KnowledgeProcessingController {
    private final KnowledgeProcessingBizService processing;
    private final KnowledgeDocumentService documents;
    private final KnowledgeChunkService chunks;
    /** @param id 资料ID @return 新轮次解析及正文预览状态；该请求执行至解析完成 */
    @AutoLog("解析知识资料并生成正文分片")
    @PostMapping("/parse")
    public ResultModel<KnowledgeDocumentVO> updateParse(@PathVariable Long id) {
        var document = processing.createPreview(id);
        var result = KnowledgeDocumentConverter.INSTANCE.toVO(document);
        return ResultModel.success(result);
    }
    /** @param id 资料ID @param current 页码 @param size 每页数量 @return 当前轮次正文分片 */
    @AutoLog("分页预览知识资料正文分片")
    @GetMapping("/chunks")
    public ResultModel<IPage<KnowledgeChunkVO>> pageChunks(@PathVariable Long id,
            @RequestParam(defaultValue = "1") @Min(1) long current,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) long size) {
        var document = documents.requireDocument(id, false);
        var page = chunks.fetchChunkPage(id, document.getProcessingVersion(), current, size);
        var result = page.convert(KnowledgeChunkConverter.INSTANCE::toVO);
        return ResultModel.success(result);
    }
    /** @param id 资料ID @return 确认后索引状态；向量模式请求执行至模型调用完成 */
    @AutoLog("确认知识资料正文分片并建立索引")
    @PostMapping("/confirm")
    public ResultModel<KnowledgeDocumentVO> updateConfirm(@PathVariable Long id) {
        var document = processing.approvePreview(id);
        var result = KnowledgeDocumentConverter.INSTANCE.toVO(document);
        return ResultModel.success(result);
    }
}
