package org.etd.ai.knowledge.controller;

import com.etd.framework.starter.client.core.permission.annotation.Permission;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.RequiredArgsConstructor;

import org.etd.ai.knowledge.biz.KnowledgeBizService;
import org.etd.ai.knowledge.controller.dto.KnowledgeFolderSaveDTO;
import org.etd.ai.knowledge.controller.vo.KnowledgeFolderVO;
import org.etd.ai.knowledge.converter.KnowledgePageConverter;
import org.etd.ai.knowledge.entity.KnowledgeFolderEntity;
import org.etd.ai.knowledge.service.*;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.etd.framework.starter.web.validation.UpdateRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 知识库目录接口，返回平铺节点供前端构建树。 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/v1/knowledge-base/{baseId}/folders")
@Permission("ai:knowledge-folder")
public class KnowledgeFolderController {
    private final KnowledgeFolderService service;
    private final KnowledgeBaseService bases;
    private final KnowledgeBizService biz;

    /**
     * 返回当前可见目录及必要祖先，navigationOnly节点不得展示写操作。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param dataStatus 可选启停状态：0停用、1启用；不传表示全部状态
     * @return 统一业务响应；code=2000表示成功，data语义见方法说明
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @AutoLog("查询知识库目录列表")
    @GetMapping
    public ResultModel<List<KnowledgeFolderVO>> list(
            @PathVariable Long baseId,
            @RequestParam(required = false)
                    @Min(BasicConstant.DataStatus.DISABLED_CODE)
                    @Max(BasicConstant.DataStatus.ENABLED_CODE)
                    Integer dataStatus) {
        bases.requireBase(baseId, false);
        var folders = service.fetchFolderList(baseId, dataStatus);
        var result = folders.stream().map(KnowledgePageConverter::toFolderVO).toList();
        return ResultModel.success(result);
    }

    /**
     * 创建同知识库目录。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param dto 经过接口参数校验的页面请求，不包含可信身份字段
     * @return 统一业务响应；code=2000表示成功，data语义见方法说明
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @AutoLog("新增知识库目录")
    @PostMapping
    public ResultModel<Long> save(
            @PathVariable Long baseId, @Valid @RequestBody KnowledgeFolderSaveDTO dto) {
        var entity = toEntity(dto);
        Long id = biz.createFolder(baseId, entity);
        return ResultModel.success(id);
    }

    /**
     * 修改名称、排序、父目录或公开状态。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @param dto 经过接口参数校验的页面请求，不包含可信身份字段
     * @return 统一业务响应；code=2000表示成功，data语义见方法说明
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @AutoLog("更新知识库目录")
    @PutMapping("/{id}")
    public ResultModel<Boolean> update(
            @PathVariable Long baseId,
            @PathVariable Long id,
            @Validated(UpdateRequest.class) @RequestBody KnowledgeFolderSaveDTO dto) {
        var e = toEntity(dto);
        e.setId(id);
        biz.modifyFolder(baseId, e, dto.getVersion());
        return ResultModel.success(true);
    }

    /**
     * 删除空目录，隐藏子项也会阻止删除。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @return 统一业务响应；code=2000表示成功，data语义见方法说明
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @AutoLog("删除知识库目录")
    @DeleteMapping("/{id}")
    public ResultModel<Boolean> remove(@PathVariable Long baseId, @PathVariable Long id) {
        biz.removeFolder(baseId, id);
        return ResultModel.success(true);
    }

    private KnowledgeFolderEntity toEntity(KnowledgeFolderSaveDTO dto) {
        var e = new KnowledgeFolderEntity();
        BeanUtils.copyProperties(dto, e);
        e.setFolderName(dto.getFolderName().trim());
        return e;
    }
}
