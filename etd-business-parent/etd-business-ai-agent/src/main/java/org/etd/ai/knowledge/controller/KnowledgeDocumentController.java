package org.etd.ai.knowledge.controller;

import org.etd.common.permission.constant.KnowledgePermissionCode;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.etd.framework.starter.client.core.permission.annotation.Permission;
import jakarta.validation.Valid;
import org.etd.ai.knowledge.controller.dto.KnowledgeDocumentStatusDTO;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.biz.KnowledgeDocumentBizService;
import org.etd.ai.knowledge.controller.dto.KnowledgeDocumentSaveDTO;
import org.etd.ai.knowledge.controller.dto.KnowledgeDocumentPageQuery;
import org.etd.ai.knowledge.controller.vo.KnowledgeDocumentVO;
import org.etd.ai.knowledge.controller.vo.KnowledgeDocumentListVO;
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
@Permission(KnowledgePermissionCode.BASE)
public class KnowledgeDocumentController {
    private final KnowledgeDocumentService documents;
    private final KnowledgeDocumentBizService biz;
    /** @param query 分页及筛选条件，批量目录优先于单目录；子孙目录由前端收集
     * @return 角色范围及租户内共享的可读取资料 */
    @AutoLog("分页查询知识资料")
    @GetMapping
    public ResultModel<IPage<KnowledgeDocumentListVO>> page(@Valid @ModelAttribute KnowledgeDocumentPageQuery query) {
        var page = documents.fetchDocumentPage(query.getCurrent(), query.getSize(), query.getFolderId(),
                query.getFolderIds(), query.getKeyword(), query.getUploadStatus(), query.getChunkStatus(), query.getKnowledgeBaseId(), query.getDataStatus());
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
    /** @param id 文档 @param dto 当前版本及目标启停 @return 修改结果 */
    @PutMapping("/{id}/status")
    @AutoLog("修改知识文档正式启停状态")
    public ResultModel<Boolean> updateStatus(@PathVariable Long id, @Valid @RequestBody KnowledgeDocumentStatusDTO dto) {
        biz.modifyDocumentStatus(id, dto.getVersion(), dto.getDataStatus());
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
    /** @param id 资料ID @return 资料及原件清理结果 */
    @AutoLog("删除知识资料")
    @DeleteMapping("/{id}")
    public ResultModel<Boolean> remove(@PathVariable Long id) {
        biz.removeDocument(id);
        return ResultModel.success(true);
    }
}
