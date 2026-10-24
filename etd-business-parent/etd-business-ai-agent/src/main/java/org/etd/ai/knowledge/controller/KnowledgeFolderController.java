package org.etd.ai.knowledge.controller;

import org.etd.common.permission.constant.KnowledgePermissionCode;
import com.etd.framework.starter.client.core.permission.annotation.Permission;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.RequiredArgsConstructor;

import org.etd.ai.knowledge.biz.KnowledgeFolderBizService;
import org.etd.ai.knowledge.controller.dto.KnowledgeFolderSaveDTO;
import org.etd.ai.knowledge.controller.vo.KnowledgeFolderVO;
import org.etd.ai.knowledge.converter.KnowledgeFolderConverter;
import org.etd.ai.knowledge.service.KnowledgeFolderService;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.etd.framework.starter.web.validation.UpdateRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 知识库文档管理内部分类接口，返回平铺节点供前端构建树。 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/v1/knowledge-folders")
@Permission(KnowledgePermissionCode.BASE)
public class KnowledgeFolderController {
    private final KnowledgeFolderService service;
    private final KnowledgeFolderBizService biz;

    /**
     * 返回当前知识库可见分类及必要祖先，维护者可查看空分类。
     *
     * @param knowledgeBaseId 必填知识库标识，所有查询限定当前库
     * @param dataStatus 可选启停状态：0停用、1启用；不传表示全部状态
     * @return 统一业务响应；code=2000表示成功，data语义见方法说明
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @AutoLog("查询知识库目录列表")
    @GetMapping
    public ResultModel<List<KnowledgeFolderVO>> list(
            @RequestParam @Min(1) Long knowledgeBaseId,
            @RequestParam(required = false)
                    @Min(BasicConstant.DataStatus.DISABLED_CODE)
                    @Max(BasicConstant.DataStatus.ENABLED_CODE)
                    Integer dataStatus) {
        var folders = service.fetchFolderList(knowledgeBaseId, dataStatus);
        var result = folders.stream().map(KnowledgeFolderConverter.INSTANCE::toVO).toList();
        return ResultModel.success(result);
    }

    /**
     * 在已授权的知识库内创建分类。
     *
     * @param dto 经过接口参数校验的页面请求，不包含可信身份字段
     * @return 统一业务响应；code=2000表示成功，data语义见方法说明
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @AutoLog("新增知识库目录")
    @PostMapping
    public ResultModel<Long> save(
            @Valid @RequestBody KnowledgeFolderSaveDTO dto) {
        var entity = KnowledgeFolderConverter.INSTANCE.toEntity(dto);
        Long id = biz.createFolder(entity);
        return ResultModel.success(id);
    }

    /**
     * 修改名称、排序或父目录。
     *
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @param dto 经过接口参数校验的页面请求，不包含可信身份字段
     * @return 统一业务响应；code=2000表示成功，data语义见方法说明
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @AutoLog("更新知识库目录")
    @PutMapping("/{id}")
    public ResultModel<Boolean> update(
            @PathVariable Long id,
            @Validated(UpdateRequest.class) @RequestBody KnowledgeFolderSaveDTO dto) {
        var e = KnowledgeFolderConverter.INSTANCE.toEntity(dto);
        e.setId(id);
        biz.modifyFolder(e, dto.getVersion());
        return ResultModel.success(true);
    }

    /**
     * 删除空目录，隐藏子项也会阻止删除。
     *
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @return 统一业务响应；code=2000表示成功，data语义见方法说明
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @AutoLog("删除知识库目录")
    @DeleteMapping("/{id}")
    public ResultModel<Boolean> remove(@PathVariable Long id) {
        biz.removeFolder(id);
        return ResultModel.success(true);
    }

}
