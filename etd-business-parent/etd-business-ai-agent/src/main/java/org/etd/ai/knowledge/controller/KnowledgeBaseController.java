package org.etd.ai.knowledge.controller;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.etd.framework.starter.client.core.permission.annotation.Permission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.biz.KnowledgeBaseBizService;
import org.etd.ai.knowledge.biz.KnowledgeRecallBizService;
import org.etd.ai.knowledge.controller.dto.KnowledgeBaseSaveDTO;
import org.etd.ai.knowledge.controller.dto.KnowledgeChunkVersionDTO;
import org.etd.ai.knowledge.controller.dto.KnowledgeRecallDTO;
import org.etd.ai.knowledge.controller.vo.KnowledgeBaseVO;
import org.etd.ai.knowledge.controller.vo.KnowledgeSearchHitVO;
import org.etd.ai.knowledge.converter.KnowledgeBaseConverter;
import org.etd.ai.knowledge.converter.KnowledgeSearchConverter;
import org.etd.ai.knowledge.service.KnowledgeBaseService;
import org.etd.common.permission.constant.KnowledgePermissionCode;
import org.etd.framework.common.core.constants.PermissionAction;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.etd.ai.knowledge.controller.vo.KnowledgeRecallResultVO;
/** 知识库卡片、设置、索引与召回入口；库内资源统一挂载此菜单权限。 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/v1/knowledge-bases")
@Permission(KnowledgePermissionCode.BASE)
public class KnowledgeBaseController {
    private final KnowledgeBaseService bases;
    private final KnowledgeBaseBizService biz;
    private final KnowledgeRecallBizService recall;
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
    /** @param id 库ID @return 配置和构建进度，共享可读不授予设置修改权限 */
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
    /** @param id 库 @param dto 当前版本 @return 后台构建代次，通过详情轮询状态 */
    @PostMapping("/{id}/index")
    @AutoLog("初始化知识库向量索引")
    public ResultModel<Long> saveIndex(@PathVariable Long id, @Valid @RequestBody KnowledgeChunkVersionDTO dto) {
        Long generation = biz.initIndex(id, dto.getVersion());
        return ResultModel.success(generation);
    }
    /** @param id 库 @param dto 问题及测试过滤 @return 测试候选；仅维护者可查询禁用资料 */
    @PostMapping("/{id}/recall-test")
    @AutoLog("测试知识库召回")
    public ResultModel<KnowledgeRecallResultVO> saveRecallTest(@PathVariable Long id, @Valid @RequestBody KnowledgeRecallDTO dto) {
        long started = System.nanoTime();
        var hits = recall.recall(id, dto.getQuery(), dto.getDataStatus(), true);
        Long elapsed = (System.nanoTime() - started) / 1_000_000;
        var result = KnowledgeSearchConverter.INSTANCE.toResult(hits, elapsed);
        return ResultModel.success(result);
    }
    /** @param id 库 @param dto 问题 @return 正式召回，仅启用资料；供后续Agent工具复用 */
    @PostMapping("/{id}/search")
    @Permission(action = PermissionAction.READ)
    @AutoLog("检索知识库正式资料")
    public ResultModel<KnowledgeRecallResultVO> getSearch(@PathVariable Long id, @Valid @RequestBody KnowledgeRecallDTO dto) {
        long started = System.nanoTime();
        var hits = recall.recall(id, dto.getQuery(), null, false);
        Long elapsed = (System.nanoTime() - started) / 1_000_000;
        var result = KnowledgeSearchConverter.INSTANCE.toResult(hits, elapsed);
        return ResultModel.success(result);
    }
}
