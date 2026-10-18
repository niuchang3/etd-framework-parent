package org.etd.ai.upload.controller;

import com.etd.framework.starter.client.core.permission.annotation.Permission;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import lombok.RequiredArgsConstructor;

import org.etd.ai.upload.biz.UploadBizService;
import org.etd.ai.upload.controller.dto.*;
import org.etd.ai.upload.controller.vo.UploadSessionVO;
import org.etd.ai.upload.entity.UploadSessionEntity;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.etd.framework.starter.storage.model.CompleteMultipartReqModel.PartETagInfo;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** MinIO分片上传控制面，文件内容由浏览器直接PUT至私有MinIO。 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/v1/knowledge-base/{baseId}/uploads")
@Permission("ai:knowledge-document")
public class UploadController {
    private final UploadBizService biz;

    /**
     * 创建新文件或显式替换上传，会话策略以后端响应为准。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param dto 经过接口参数校验的页面请求，不包含可信身份字段
     * @return 统一业务响应；code=2000表示成功，data语义见方法说明
     * @throws Exception 对象存储请求或文件内容核验失败时
     */
    @AutoLog("创建知识库文件上传会话")
    @PostMapping
    public ResultModel<UploadSessionVO> save(
            @PathVariable Long baseId, @Valid @RequestBody UploadInitDTO dto) throws Exception {
        var e = new UploadSessionEntity();
        BeanUtils.copyProperties(dto, e);
        var session = biz.createUpload(baseId, e, dto.getSessionExpireSeconds());
        var v = new UploadSessionVO();
        BeanUtils.copyProperties(session, v);
        v.setTaskId(session.getId());
        return ResultModel.success(v);
    }

    /**
     * 申请单片URL，可在过期后重新申请，编号不得超过后端partCount。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @param number 从1开始的分片序号，不能超过任务总片数
     * @return 统一业务响应；code=2000表示成功，data语义见方法说明
     * @throws Exception 对象存储请求或文件内容核验失败时
     */
    @AutoLog("获取知识库文件分片上传链接")
    @PostMapping("/{id}/parts/{number}/url")
    public ResultModel<Map<String, Object>> savePartUrl(
            @PathVariable Long baseId,
            @PathVariable Long id,
            @PathVariable @Min(1) @Max(10000) int number)
            throws Exception {
        String url = biz.createPartUrl(baseId, id, number);
        Map<String, Object> result = Map.of("url", url, "partNumber", number);
        return ResultModel.success(result);
    }

    /**
     * 校验实际存储分片及原件内容后登记文件，重复提交返回同一documentId。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @param dto 经过接口参数校验的页面请求，不包含可信身份字段
     * @return 统一业务响应；code=2000表示成功，data语义见方法说明
     * @throws Exception 对象存储请求或文件内容核验失败时
     */
    @AutoLog("确认知识库文件上传完成")
    @PostMapping("/{id}/complete")
    public ResultModel<Map<String, Long>> saveCompletion(
            @PathVariable Long baseId,
            @PathVariable Long id,
            @Valid @RequestBody UploadCompleteDTO dto)
            throws Exception {
        var parts =
                dto.getParts().stream()
                        .map(p -> new PartETagInfo(p.getPartNumber(), p.getEtag()))
                        .toList();
        Long documentId = biz.completeUpload(baseId, id, parts);
        var result = Map.of("documentId", documentId);
        return ResultModel.success(result);
    }

    /**
     * 取消未完成上传，不删除已完成文件，清理异步重试。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @return 统一业务响应；code=2000表示成功，data语义见方法说明
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @AutoLog("取消知识库文件上传")
    @DeleteMapping("/{id}")
    public ResultModel<Boolean> remove(@PathVariable Long baseId, @PathVariable Long id) {
        biz.cancelUpload(baseId, id);
        return ResultModel.success(true);
    }
}
