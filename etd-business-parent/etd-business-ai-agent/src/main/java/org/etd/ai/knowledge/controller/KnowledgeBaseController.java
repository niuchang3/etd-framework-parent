package org.etd.ai.knowledge.controller;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.etd.framework.starter.client.core.permission.annotation.Permission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.biz.KnowledgeBaseBizService;
import org.etd.ai.knowledge.controller.dto.KnowledgeBaseSaveDTO;
import org.etd.ai.knowledge.controller.vo.KnowledgeBaseVO;
import org.etd.ai.knowledge.converter.KnowledgeBaseConverter;
import org.etd.ai.knowledge.service.KnowledgeBaseService;
import org.etd.common.permission.constant.KnowledgePermissionCode;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
/** 知识库卡片与设置入口；库内资源统一挂载此菜单权限。 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/v1/knowledge-bases")
@Permission(KnowledgePermissionCode.BASE)
public class KnowledgeBaseController {
    private final KnowledgeBaseService bases;
    private final KnowledgeBaseBizService biz;
    /** @param current 页码 @param size 页大小 @param keyword 名称 @return 可见知识库卡片 */
    @GetMapping
    @AutoLog("分页查询知识库")
    public ResultModel<IPage<KnowledgeBaseVO>> page(@RequestParam(defaultValue = "1") @Min(1) long current,
            @RequestParam(defaultValue = "12") @Min(1) @Max(200) long size,
            @RequestParam(required = false) @Size(max = 100) String keyword) {
        var page = bases.fetchBasePage(current, size, keyword);
        var result = page.convert(KnowledgeBaseConverter.INSTANCE::toVO);
        return ResultModel.success(result);
    }
    /** @param id 库ID @return 知识库配置，共享可读不授予设置修改权限 */
    @GetMapping("/{id}")
    @AutoLog("查询知识库详情")
    public ResultModel<KnowledgeBaseVO> get(@PathVariable Long id) {
        var base = bases.requireBase(id, false, false);
        var result = KnowledgeBaseConverter.INSTANCE.toVO(base);
        return ResultModel.success(result);
    }
    /** @param dto 完整设置 @return 新知识库ID */
    @PostMapping
    @AutoLog("创建知识库")
    public ResultModel<Long> save(@Valid @RequestBody KnowledgeBaseSaveDTO dto) {
        var input = KnowledgeBaseConverter.INSTANCE.toEntity(dto);
        Long id = biz.createBase(input);
        return ResultModel.success(id);
    }
    /** @param id 库 @param dto 完整设置，version必填 @return 操作结果 */
    @PutMapping("/{id}")
    @AutoLog("修改知识库设置")
    public ResultModel<Boolean> update(@PathVariable Long id, @Valid @RequestBody KnowledgeBaseSaveDTO dto) {
        var input = KnowledgeBaseConverter.INSTANCE.toEntity(dto);
        biz.modifyBase(id, input, dto.getVersion());
        return ResultModel.success(true);
    }
    /** @param id 空库 @return 操作结果 */
    @DeleteMapping("/{id}")
    @AutoLog("删除知识库")
    public ResultModel<Boolean> remove(@PathVariable Long id) {
        biz.removeBase(id);
        return ResultModel.success(true);
    }
}
