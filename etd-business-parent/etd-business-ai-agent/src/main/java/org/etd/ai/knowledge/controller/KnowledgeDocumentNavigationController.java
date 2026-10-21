package org.etd.ai.knowledge.controller;

import org.etd.common.permission.constant.KnowledgePermissionCode;
import com.etd.framework.starter.client.core.permission.annotation.Permission;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.controller.vo.KnowledgeFolderVO;
import org.etd.ai.knowledge.converter.KnowledgeFolderConverter;
import org.etd.ai.knowledge.service.KnowledgeFolderService;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** 知识资料页面的只读目录导航，与分类维护菜单分别授权，复用同一目录和数据权限。 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/v1/knowledge-documents/folders")
@Permission(KnowledgePermissionCode.DOCUMENT)
public class KnowledgeDocumentNavigationController {
    private final KnowledgeFolderService folders;
    /** @param orgId 可选部门 @param dataStatus 可选启停状态 @return 可见目录及只供导航的必要祖先 */
    @AutoLog("查询知识资料分类导航")
    @GetMapping
    public ResultModel<List<KnowledgeFolderVO>> list(@RequestParam(required = false) @Positive Long orgId,
            @RequestParam(required = false) @Min(BasicConstant.DataStatus.DISABLED_CODE)
            @Max(BasicConstant.DataStatus.ENABLED_CODE) Integer dataStatus) {
        var entities = folders.fetchFolderList(orgId, dataStatus);
        var result = entities.stream().map(KnowledgeFolderConverter.INSTANCE::toVO).toList();
        return ResultModel.success(result);
    }
}
