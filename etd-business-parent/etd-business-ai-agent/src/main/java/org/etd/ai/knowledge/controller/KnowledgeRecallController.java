package org.etd.ai.knowledge.controller;

import com.etd.framework.starter.client.core.permission.annotation.Permission;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.biz.KnowledgeRecallBizService;
import org.etd.ai.knowledge.controller.dto.KnowledgeRecallDTO;
import org.etd.ai.knowledge.controller.vo.KnowledgeRecallHitVO;
import org.etd.ai.knowledge.converter.KnowledgeRecallConverter;
import org.etd.common.permission.constant.KnowledgePermissionCode;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.framework.common.core.constants.PermissionAction;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.etd.ai.knowledge.model.KnowledgeRecallHit;

/** 知识库召回测试入口，仅查询权限内已确认资料，允许资料尚未启用。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/knowledge-bases/{id}")
@Permission(value = KnowledgePermissionCode.BASE, action = PermissionAction.READ)
public class KnowledgeRecallController {
    private final KnowledgeRecallBizService biz;

    /** @param id 知识库ID @param dto 检索问题 @return 按最终分数排序的召回测试命中
     * @throws ApiRuntimeException 无读取权限或模型不可用 */
    @PostMapping("/recall-test")
    @AutoLog("测试知识库召回效果")
    public ResultModel<List<KnowledgeRecallHitVO>> getTestRecall(@PathVariable Long id,
            @Valid @RequestBody KnowledgeRecallDTO dto) {
        List<KnowledgeRecallHit> hits = biz.testRecall(id, dto.getQuery());
        List<KnowledgeRecallHitVO> result = KnowledgeRecallConverter.INSTANCE.toVOList(hits);
        return ResultModel.success(result);
    }

}
