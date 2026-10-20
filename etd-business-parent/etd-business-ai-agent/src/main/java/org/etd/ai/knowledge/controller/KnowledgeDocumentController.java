package org.etd.ai.knowledge.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.etd.framework.starter.client.core.permission.annotation.Permission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.constant.KnowledgePolicy;
import org.etd.ai.knowledge.biz.KnowledgeDocumentBizService;
import org.etd.ai.knowledge.controller.dto.KnowledgeDocumentSaveDTO;
import org.etd.ai.knowledge.controller.vo.KnowledgeDocumentVO;
import org.etd.ai.knowledge.controller.vo.KnowledgeDocumentListVO;
import org.etd.ai.knowledge.controller.vo.KnowledgeEmbeddingOptionVO;
import org.etd.ai.model.service.ModelConnectionService;
import java.util.List;
import org.etd.ai.knowledge.converter.KnowledgeDocumentConverter;
import org.etd.ai.knowledge.service.KnowledgeDocumentService;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.framework.common.core.constants.PermissionAction;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 知识资料列表、配置与原件管理入口。 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/v1/knowledge-documents")
@Permission(KnowledgePolicy.DOCUMENT_PERMISSION)
public class KnowledgeDocumentController {
    private final KnowledgeDocumentService documents;
    private final KnowledgeDocumentBizService biz;
    private final ModelConnectionService connections;
    /** @return 已启用向量模型连接器，只有资料read权限即可读取脱敏选择项 */
    @AutoLog("查询知识资料可选向量模型")
    @GetMapping("/model-options")
    public ResultModel<List<KnowledgeEmbeddingOptionVO>> listModelOptions() {
        var options = connections.fetchEmbeddingConnectionOptionList(KnowledgePolicy.DIMENSIONS);
        var result = options.stream().map(KnowledgeDocumentConverter.INSTANCE::toVO).toList();
        return ResultModel.success(result);
    }

    /** @param current 页码 @param size 每页数量 @param folderId 可选目录
     * @param keyword 名称搜索 @param uploadStatus 上传状态 @param processingStatus 处理状态 @return 当前管理范围内资料 */
    @AutoLog("分页查询知识资料")
    @GetMapping
    public ResultModel<IPage<KnowledgeDocumentListVO>> page(@RequestParam(defaultValue = "1") @Min(1) long current,
            @RequestParam(defaultValue = "10") @Min(1) @Max(200) long size,
            @RequestParam(required = false) @Positive Long folderId,
            @RequestParam(required = false) @Size(max = 255) String keyword,
            @RequestParam(required = false) String uploadStatus,
            @RequestParam(required = false) String processingStatus) {
        var page = documents.fetchDocumentPage(current, size, folderId, keyword, uploadStatus, processingStatus);
        var result = page.convert(KnowledgeDocumentConverter.INSTANCE::toListVO);
        return ResultModel.success(result);
    }
    /** @param id 资料ID @return 资料及上传进度详情 */
    @AutoLog("查询知识资料详情")
    @GetMapping("/{id}")
    public ResultModel<KnowledgeDocumentVO> get(@PathVariable Long id) {
        var document = documents.requireDocument(id, false);
        var result = KnowledgeDocumentConverter.INSTANCE.toVO(document);
        return ResultModel.success(result);
    }
    /** @param id 资料ID @param dto 可编辑配置；目录移动不改变资料归属部门 @return 修改结果 */
    @AutoLog("更新知识资料配置")
    @PutMapping("/{id}")
    public ResultModel<Boolean> update(@PathVariable Long id, @Valid @RequestBody KnowledgeDocumentSaveDTO dto) {
        var input = KnowledgeDocumentConverter.INSTANCE.toEntity(dto);
        biz.modifyDocument(id, input, dto.getVersion());
        return ResultModel.success(true);
    }
    /** @param id 资料ID @return 临时下载地址，不得长期保存 */
    @AutoLog("下载知识资料原件")
    @Permission(action = PermissionAction.READ)
    @PostMapping("/{id}/download-url")
    public ResultModel<String> getDownloadUrl(@PathVariable Long id) {
        String url = biz.createDownloadUrl(id);
        return ResultModel.success(url);
    }
    /** @param id 资料ID @return 原件、正文及向量清理结果 */
    @AutoLog("删除知识资料")
    @DeleteMapping("/{id}")
    public ResultModel<Boolean> remove(@PathVariable Long id) {
        biz.removeDocument(id);
        return ResultModel.success(true);
    }
}
