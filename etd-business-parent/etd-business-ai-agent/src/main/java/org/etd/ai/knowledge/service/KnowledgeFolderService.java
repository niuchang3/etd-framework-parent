package org.etd.ai.knowledge.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import lombok.RequiredArgsConstructor;

import org.etd.ai.knowledge.entity.KnowledgeFolderEntity;
import org.etd.ai.knowledge.mapper.KnowledgeFolderMapper;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermission;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermissionTable;
import org.springframework.stereotype.Service;

import java.util.List;

/** 目录树读取及单目录管理，结构锁由业务编排层持有。 */
@Service
@DataPermission(tables = @DataPermissionTable(table = "ai_knowledge_folder"))
@RequiredArgsConstructor
public class KnowledgeFolderService {
    private final KnowledgeFolderMapper mapper;

    /**
     * 取得可读节点及只用于导航的祖先节点。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param dataStatus 可选目录状态；导航祖先不受此筛选限制
     * @return 符合条件的记录集合，不在Java中拼接跨表权限数据
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    // f是可读目录来源；entry及p仅补齐导航，不能对它们追加相同权限而截断路径。
    @DataPermission(
            tables = {
                @DataPermissionTable(
                        table = "ai_knowledge_folder",
                        alias = "f",
                        publicColumn = "is_public"),
                @DataPermissionTable(table = "ai_knowledge_document", publicColumn = "is_public")
            })
    public List<KnowledgeFolderEntity> fetchFolderList(Long baseId, Integer dataStatus) {
        return mapper.selectReadableFolderList(baseId, dataStatus);
    }

    /**
     * 管理入口使用框架原有角色范围，不使用公开标记。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @return 通过存在性及访问范围校验的业务记录
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public KnowledgeFolderEntity requireWritable(Long baseId, Long id) {
        var query =
                Wrappers.<KnowledgeFolderEntity>lambdaQuery()
                        .eq(KnowledgeFolderEntity::getId, id)
                        .eq(KnowledgeFolderEntity::getKnowledgeBaseId, baseId);
        var e = mapper.selectOne(query);
        if (e == null) {
            throw new ApiRuntimeException("目录不存在或无管理权限");
        }
        return e;
    }

    /**
     * 校验父目录可写，空表示知识库根目录。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param parentId 目标父目录标识，空表示根目录
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public void requireParent(Long baseId, Long parentId) {
        if (parentId != null) {
            requireWritable(baseId, parentId);
        }
    }

    /**
     * 判断目标是否在当前节点后代链上，防止目录循环。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @param targetId 待移动到的目标目录标识
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public void requireAcyclic(Long baseId, Long id, Long targetId) {
        if (targetId != null && mapper.selectAncestorCount(baseId, id, targetId) > 0) {
            throw new ApiRuntimeException("不能移入自身或下级目录");
        }
    }

    /**
     * 新增目录，组织及公开默认值使用框架自动填充。
     *
     * @param e 本次写入的业务记录，租户和审计信息由后端控制
     * @return 业务记录的雪花标识
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public Long createFolder(KnowledgeFolderEntity e) {
        mapper.insert(e);
        return e.getId();
    }

    /**
     * 修改目录时显式更新parent_id，支持从子目录移动到根目录。
     *
     * @param e 本次写入的业务记录，租户和审计信息由后端控制
     * @param version 页面读取的当前版本，用于拒绝并发覆盖
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public void modifyFolder(KnowledgeFolderEntity e, Long version) {
        e.setVersion(version);
        int changed = mapper.updateById(e);
        if (changed != 1) {
            throw new ApiRuntimeException("目录已变化，请刷新后重试");
        }
    }

    /**
     * 禁止删除仍有任意未删除子项的目录，包含不可见子项。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public void removeFolder(Long baseId, Long id) {
        if (mapper.selectChildCount(baseId, id) > 0) {
            throw new ApiRuntimeException("目录非空，不能删除");
        }
        mapper.deleteById(id);
    }

    /**
     * 供知识库删除前检查目录占用。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @return 符合当前租户及查询条件的记录数量
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public long countFolder(Long baseId) {
        return mapper.selectChildCount(baseId, null);
    }
}
