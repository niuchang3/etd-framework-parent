package org.etd.ai.knowledge.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.etd.framework.starter.client.core.permission.annotation.Permission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.biz.KnowledgeChunkGenerationBizService;
import org.etd.ai.knowledge.biz.KnowledgeChunkEditBizService;
import org.etd.ai.knowledge.constant.KnowledgeChunkLimits;
import org.etd.ai.knowledge.controller.dto.*;
import org.etd.ai.knowledge.controller.vo.*;
import org.etd.ai.knowledge.converter.KnowledgeChunkConverter;
import org.etd.ai.knowledge.service.KnowledgeDocumentService;
import org.etd.ai.knowledge.service.KnowledgeChunkService;
import org.etd.common.permission.constant.KnowledgePermissionCode;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 分片工作页入口，生成、预览、修改、合并及确认均为用户显式操作。 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/v1/knowledge-documents/{id}/chunks")
@Permission(KnowledgePermissionCode.DOCUMENT)
public class KnowledgeChunkController {
    private final KnowledgeDocumentService documents;
    private final KnowledgeChunkService chunks;
    private final KnowledgeChunkGenerationBizService generation;
    private final KnowledgeChunkEditBizService editing;

    /** @param id 资料ID @return 分片状态、最近成功参数和全部写操作使用的最新版本 */
    @GetMapping("/state")
    @AutoLog("查询知识资料分片工作页状态")
    public ResultModel<KnowledgeChunkStateVO> getState(@PathVariable Long id) {
        var document = documents.requireDocument(id, false);
        var result = KnowledgeChunkConverter.INSTANCE.toStateVO(document);
        return ResultModel.success(result);
    }

    /** @param id 资料ID @param dto 版本及三级参数 @return 生成完成或失败后的状态，失败不清理旧分片 */
    @PostMapping("/generate")
    @AutoLog("生成知识资料分片草稿")
    public ResultModel<KnowledgeChunkStateVO> saveGenerate(@PathVariable Long id, @Valid @RequestBody KnowledgeChunkGenerateDTO dto) {
        var document = generation.generateChunks(id, dto.getVersion(), dto.getParameters());
        var result = KnowledgeChunkConverter.INSTANCE.toStateVO(document);
        return ResultModel.success(result);
    }

    /** @param id 资料ID @param current 页码 @param size 单页数量 @return 按原文顺序的当前分片 */
    @GetMapping
    @AutoLog("分页预览知识资料分片")
    public ResultModel<IPage<KnowledgeChunkVO>> page(@PathVariable Long id,
            @RequestParam(defaultValue = "1") @Min(1) long current,
            @RequestParam(defaultValue = "20") @Min(1) @Max(KnowledgeChunkLimits.MAX_EDIT_BATCH) long size) {
        documents.requireDocument(id, false);
        var page = chunks.fetchChunkPage(id, current, size);
        var result = page.convert(KnowledgeChunkConverter.INSTANCE::toVO);
        return ResultModel.success(result);
    }

    /** @param id 资料ID @param chunkId 分片ID @return 该资料下的单片完整正文和来源 */
    @GetMapping("/{chunkId}")
    @AutoLog("查询知识资料分片详情")
    public ResultModel<KnowledgeChunkVO> get(@PathVariable Long id, @PathVariable Long chunkId) {
        documents.requireDocument(id, false);
        var chunk = chunks.requireChunk(id, chunkId);
        var result = KnowledgeChunkConverter.INSTANCE.toVO(chunk);
        return ResultModel.success(result);
    }

    /** @param id 资料ID @param dto 当前版本和本次编辑内容 @return 保存后的待确认状态 */
    @PutMapping
    @AutoLog("保存知识资料分片修改")
    public ResultModel<KnowledgeChunkStateVO> update(@PathVariable Long id, @Valid @RequestBody KnowledgeChunkSaveDTO dto) {
        var values = KnowledgeChunkConverter.INSTANCE.toEditValues(dto.getChunks());
        var document = editing.modifyChunks(id, dto.getVersion(), values);
        var result = KnowledgeChunkConverter.INSTANCE.toStateVO(document);
        return ResultModel.success(result);
    }

    /** @param id 资料ID @param dto 当前版本和按顺序选择的相邻分片 @return 合并后的待确认状态 */
    @PostMapping("/merge")
    @AutoLog("合并知识资料相邻分片")
    public ResultModel<KnowledgeChunkStateVO> updateMerge(@PathVariable Long id, @Valid @RequestBody KnowledgeChunkMergeDTO dto) {
        var document = editing.mergeChunks(id, dto.getVersion(), dto.getChunkIds());
        var result = KnowledgeChunkConverter.INSTANCE.toStateVO(document);
        return ResultModel.success(result);
    }

    /** @param id 资料ID @param dto 当前版本 @return 用户确认结果，不触发向量处理 */
    @PostMapping("/confirm")
    @AutoLog("确认知识资料分片结果")
    public ResultModel<KnowledgeChunkStateVO> updateConfirm(@PathVariable Long id, @Valid @RequestBody KnowledgeChunkVersionDTO dto) {
        var document = editing.confirmChunks(id, dto.getVersion());
        var result = KnowledgeChunkConverter.INSTANCE.toStateVO(document);
        return ResultModel.success(result);
    }
}
