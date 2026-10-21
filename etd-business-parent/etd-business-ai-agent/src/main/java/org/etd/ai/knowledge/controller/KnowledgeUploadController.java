package org.etd.ai.knowledge.controller;

import org.etd.common.permission.constant.KnowledgePermissionCode;
import com.etd.framework.starter.client.core.permission.annotation.Permission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.biz.KnowledgeUploadBizService;
import org.etd.ai.knowledge.controller.dto.*;
import org.etd.ai.knowledge.controller.vo.KnowledgeDocumentVO;
import org.etd.ai.knowledge.controller.vo.KnowledgeUploadPolicyVO;
import org.etd.ai.knowledge.config.KnowledgeUploadProperties;
import org.etd.ai.knowledge.converter.KnowledgeDocumentConverter;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 文件直传入口；不接受客户端指定存储桶、对象键或MinIO会话ID。 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/v1/knowledge-documents/uploads")
@Permission(KnowledgePermissionCode.DOCUMENT)
public class KnowledgeUploadController {
    private final KnowledgeUploadBizService uploads;
    private final KnowledgeUploadProperties properties;

    /** @return 上传技术策略，文件扩展名请读取UPMS系统参数ai.knowledge.upload */
    @AutoLog("查询知识资料上传策略")
    @GetMapping("/policy")
    public ResultModel<KnowledgeUploadPolicyVO> getPolicy() {
        var result = new KnowledgeUploadPolicyVO(properties.getMaxFileBytes(), properties.getPartSizeBytes(),
                properties.getSessionSeconds(), properties.getUrlSeconds());
        return ResultModel.success(result);
    }


    /** @param dto 原件身份及目录 @return 初始化资料详情 */
    @AutoLog("初始化知识资料分片上传")
    @PostMapping
    public ResultModel<KnowledgeDocumentVO> save(@Valid @RequestBody KnowledgeUploadInitDTO dto) {
        var entity = KnowledgeDocumentConverter.INSTANCE.toEntity(dto);
        var document = uploads.createUpload(entity);
        var result = KnowledgeDocumentConverter.INSTANCE.toVO(document);
        return ResultModel.success(result);
    }

    /** @param id 资料ID @param number 分片编号 @return 临时PUT地址，不得记录或长期保存 */
    @AutoLog("申请知识资料上传分片地址")
    @PostMapping("/{id}/parts/{number}/url")
    public ResultModel<String> getPartUrl(@PathVariable Long id, @PathVariable @Min(1) int number) {
        String url = uploads.createPartUrl(id, number);
        return ResultModel.success(url);
    }

    /** @param id 资料ID @param number 分片编号 @param dto 上传回执 @return 最新进度 */
    @AutoLog("确认知识资料上传分片")
    @PutMapping("/{id}/parts/{number}")
    public ResultModel<KnowledgeDocumentVO> updatePart(@PathVariable Long id, @PathVariable @Min(1) int number,
            @Valid @RequestBody KnowledgePartConfirmDTO dto) {
        var document = uploads.confirmPart(id, number, dto.getEtag());
        var result = KnowledgeDocumentConverter.INSTANCE.toVO(document);
        return ResultModel.success(result);
    }

    /** @param id 资料ID @param dto 本地文件身份 @return 核对后的已确认分片清单 */
    @AutoLog("恢复知识资料断点上传")
    @PostMapping("/{id}/resume")
    public ResultModel<KnowledgeDocumentVO> updateResume(@PathVariable Long id, @Valid @RequestBody KnowledgeResumeDTO dto) {
        var document = uploads.resumeUpload(id, dto.getFileSha256());
        var result = KnowledgeDocumentConverter.INSTANCE.toVO(document);
        return ResultModel.success(result);
    }

    /** @param id 资料ID @return 完整文件核验结果；重复调用幂等 */
    @AutoLog("合并知识资料上传文件")
    @PostMapping("/{id}/complete")
    public ResultModel<KnowledgeDocumentVO> updateComplete(@PathVariable Long id) {
        var document = uploads.completeUpload(id);
        var result = KnowledgeDocumentConverter.INSTANCE.toVO(document);
        return ResultModel.success(result);
    }

    /** @param id 资料ID @return 取消结果，重复取消幂等 */
    @AutoLog("取消知识资料分片上传")
    @PostMapping("/{id}/cancel")
    public ResultModel<Boolean> updateCancel(@PathVariable Long id) {
        uploads.cancelUpload(id);
        return ResultModel.success(true);
    }
}
