package org.etd.ai.document.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.etd.framework.starter.client.core.permission.annotation.Permission;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import lombok.RequiredArgsConstructor;

import org.etd.ai.document.biz.KnowledgeDocumentBizService;
import org.etd.ai.document.controller.dto.DocumentUpdateDTO;
import org.etd.ai.document.controller.vo.KnowledgeDocumentVO;
import org.etd.ai.document.entity.KnowledgeDocumentEntity;
import org.etd.ai.document.service.KnowledgeDocumentService;
import org.etd.ai.knowledge.converter.KnowledgePageConverter;
import org.etd.ai.knowledge.service.KnowledgeBaseService;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 已上传文件的列表、预览下载和管理接口。 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/v1/knowledge-base/{baseId}/documents")
@Permission("ai:knowledge-document")
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService service;
    private final KnowledgeDocumentBizService biz;
    private final KnowledgeBaseService bases;

    /**
     * folderId省略表示全部；uncategorized=true只显示未分类，两者不能同时指定。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param folderId 目录标识，空表示未分类或全库统计，具体见方法用途
     * @param uncategorized 是否仅查询未分类文件，不能与目录筛选并用
     * @param keyword 可选名称关键字，不作为SQL片段执行
     * @param current 从1开始的页码
     * @param size 每页记录数或预期字节数，以方法用途为准
     * @param dataStatus 可选启停状态：0停用、1启用；不传表示全部状态
     * @return 统一业务响应；code=2000表示成功，data语义见方法说明
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @AutoLog("分页查询知识库文档列表")
    @GetMapping
    public ResultModel<IPage<KnowledgeDocumentVO>> page(
            @PathVariable Long baseId,
            @RequestParam(required = false) Long folderId,
            @RequestParam(defaultValue = "false") boolean uncategorized,
            @RequestParam(required = false) @Size(max = 255) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) long current,
            @RequestParam(defaultValue = "10") @Min(1) @Max(200) long size,
            @RequestParam(required = false)
                    @Min(BasicConstant.DataStatus.DISABLED_CODE)
                    @Max(BasicConstant.DataStatus.ENABLED_CODE)
                    Integer dataStatus) {
        bases.requireBase(baseId, false);
        if (folderId != null && uncategorized) {
            throw new ApiRuntimeException("目录与未分类筛选不能同时指定");
        }
        var page =
                service.fetchPage(
                        baseId, folderId, uncategorized, keyword, current, size, dataStatus);
        var result = page.convert(KnowledgePageConverter::toDocumentVO);
        return ResultModel.success(result);
    }

    /**
     * 获取文件详情，不返回对象键或存储凭证。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @return 统一业务响应；code=2000表示成功，data语义见方法说明
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @AutoLog("获取知识库文档详情")
    @GetMapping("/{id}")
    public ResultModel<KnowledgeDocumentVO> get(@PathVariable Long baseId, @PathVariable Long id) {
        var entity = service.requireReadable(baseId, id);
        var result = KnowledgePageConverter.toDocumentVO(entity);
        return ResultModel.success(result);
    }

    /**
     * 获取原件短期访问链接；预览由浏览器根据文件类型决定，不提供文本分片预览。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @return 统一业务响应；code=2000表示成功，data语义见方法说明
     * @throws Exception 对象存储请求或文件内容核验失败时
     */
    @AutoLog("获取知识库文档原件访问链接")
    @GetMapping("/{id}/download-url")
    public ResultModel<Map<String, Object>> getDownloadUrl(
            @PathVariable Long baseId, @PathVariable Long id) throws Exception {
        String url = biz.createDownloadUrl(baseId, id);
        Map<String, Object> result =
                Map.of(
                        "url",
                        url,
                        "expiresInSeconds",
                        KnowledgeDocumentBizService.DOWNLOAD_EXPIRY_SECONDS);
        return ResultModel.success(result);
    }

    /**
     * 修改显示名称、分类及公开状态，文件内容替换使用上传接口。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @param dto 经过接口参数校验的页面请求，不包含可信身份字段
     * @return 统一业务响应；code=2000表示成功，data语义见方法说明
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @AutoLog("更新知识库文档")
    @PutMapping("/{id}")
    public ResultModel<Boolean> update(
            @PathVariable Long baseId,
            @PathVariable Long id,
            @Valid @RequestBody DocumentUpdateDTO dto) {
        var e = new KnowledgeDocumentEntity();
        BeanUtils.copyProperties(dto, e);
        e.setId(id);
        biz.modifyDocument(baseId, e, dto.getVersion());
        return ResultModel.success(true);
    }

    /**
     * 删除文件并登记原件清理任务。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @return 统一业务响应；code=2000表示成功，data语义见方法说明
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @AutoLog("删除知识库文档")
    @DeleteMapping("/{id}")
    public ResultModel<Boolean> remove(@PathVariable Long baseId, @PathVariable Long id) {
        biz.removeDocument(baseId, id);
        return ResultModel.success(true);
    }
}
