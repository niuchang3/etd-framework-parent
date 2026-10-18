package org.etd.ai.knowledge.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.etd.framework.starter.client.core.permission.annotation.Permission;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import lombok.RequiredArgsConstructor;

import org.etd.ai.knowledge.biz.KnowledgeBizService;
import org.etd.ai.knowledge.controller.dto.KnowledgeBaseSaveDTO;
import org.etd.ai.knowledge.controller.vo.KnowledgeBaseVO;
import org.etd.ai.knowledge.entity.KnowledgeBaseEntity;
import org.etd.ai.knowledge.service.KnowledgeBaseService;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.etd.framework.starter.web.validation.UpdateRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 知识库页面基础接口；不提供解析、向量化或检索执行入口。 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/v1/knowledge-base")
@Permission("ai:knowledge-base")
public class KnowledgeBaseController {
    private final KnowledgeBaseService service;
    private final KnowledgeBizService biz;

    /**
     * 按当前租户分页，不附带任何未授权文件数量。
     *
     * @param current 从1开始的页码
     * @param size 每页记录数或预期字节数，以方法用途为准
     * @param keyword 可选名称关键字，不作为SQL片段执行
     * @param dataStatus 可选启停状态：0停用、1启用；不传表示全部状态
     * @return 统一业务响应；code=2000表示成功，data语义见方法说明
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @AutoLog("分页查询知识库列表")
    @GetMapping
    public ResultModel<IPage<KnowledgeBaseVO>> page(
            @RequestParam(defaultValue = "1") @Min(1) long current,
            @RequestParam(defaultValue = "10") @Min(1) @Max(200) long size,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false)
                    @Min(BasicConstant.DataStatus.DISABLED_CODE)
                    @Max(BasicConstant.DataStatus.ENABLED_CODE)
                    Integer dataStatus) {
        var page = service.fetchPage(current, size, keyword, dataStatus);
        var result = page.convert(this::toVO);
        return ResultModel.success(result);
    }

    /**
     * 获取知识库配置，租户在后端校验。
     *
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @return 统一业务响应；code=2000表示成功，data语义见方法说明
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @AutoLog("获取知识库详情")
    @GetMapping("/{id}")
    public ResultModel<KnowledgeBaseVO> get(@PathVariable Long id) {
        var entity = service.requireBase(id, false);
        var result = toVO(entity);
        return ResultModel.success(result);
    }

    /**
     * 创建知识库草稿，不验证或调用模型。
     *
     * @param dto 经过接口参数校验的页面请求，不包含可信身份字段
     * @return 统一业务响应；code=2000表示成功，data语义见方法说明
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @AutoLog("新增知识库")
    @PostMapping
    public ResultModel<Long> save(@Valid @RequestBody KnowledgeBaseSaveDTO dto) {
        var entity = toEntity(dto);
        Long id = service.createBase(entity);
        return ResultModel.success(id);
    }

    /**
     * 更新配置需要提交版本；本阶段模型绑定尚未开放。
     *
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @param dto 经过接口参数校验的页面请求，不包含可信身份字段
     * @return 统一业务响应；code=2000表示成功，data语义见方法说明
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @AutoLog("更新知识库")
    @PutMapping("/{id}")
    public ResultModel<Boolean> update(
            @PathVariable Long id,
            @Validated(UpdateRequest.class) @RequestBody KnowledgeBaseSaveDTO dto) {
        var e = toEntity(dto);
        e.setId(id);
        service.modifyBase(e, dto.getVersion());
        return ResultModel.success(true);
    }

    /**
     * 非空知识库不可删除。
     *
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @return 统一业务响应；code=2000表示成功，data语义见方法说明
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @AutoLog("删除知识库")
    @DeleteMapping("/{id}")
    public ResultModel<Boolean> remove(@PathVariable Long id) {
        biz.removeBase(id);
        return ResultModel.success(true);
    }

    private KnowledgeBaseEntity toEntity(KnowledgeBaseSaveDTO dto) {
        var e = new KnowledgeBaseEntity();
        BeanUtils.copyProperties(dto, e);
        e.setKnowledgeName(dto.getKnowledgeName().trim());
        return e;
    }

    private KnowledgeBaseVO toVO(KnowledgeBaseEntity entity) {
        var vo = new KnowledgeBaseVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
