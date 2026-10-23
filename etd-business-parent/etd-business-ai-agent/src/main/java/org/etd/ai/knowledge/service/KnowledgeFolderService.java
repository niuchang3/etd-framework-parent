package org.etd.ai.knowledge.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.constant.KnowledgePolicy;
import org.etd.ai.knowledge.entity.KnowledgeFolderEntity;
import org.etd.ai.knowledge.mapper.KnowledgeFolderMapper;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermission;
import org.etd.framework.starter.mybaits.permission.model.DataPermissionScope;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermissionTable;

import java.util.List;

/** 按前端所选部门维护独立目录树，不校验UPMS部门。 */
@Service
@DataPermission(tables = @DataPermissionTable(table = "ai_knowledge_folder"))
@RequiredArgsConstructor
public class KnowledgeFolderService {
    private final KnowledgeFolderMapper mapper;

    /**
     * 查询权限范围内目录及必要祖先，可按部门进一步筛选。
     * @param orgId 可选部门标识，空时不按部门筛选，始终遵循框架数据权限
     * @param dataStatus 可选目录状态，必要祖先不受筛选限制
     * @return 平铺目录节点列表
     */
    @DataPermission(tables = @DataPermissionTable(table = "ai_knowledge_folder", alias = "f"))
    public List<KnowledgeFolderEntity> fetchFolderList(Long orgId, Integer dataStatus) {
        return mapper.selectFolderList(orgId, dataStatus);
    }

    /**
     * 资料页导航同时补齐共享资料所在目录及祖先，不授予这些目录维护权限。
     * @param orgId 可选目录部门
     * @param dataStatus 可选目录状态
     * @return 角色范围目录和共享资料所在路径
     */
    @DataPermission(tables = {
        @DataPermissionTable(table = "ai_knowledge_folder", alias = "f"),
        @DataPermissionTable(table = "ai_knowledge_document", alias = "d", publicColumn = KnowledgePolicy.PUBLIC_COLUMN)
    })
    public List<KnowledgeFolderEntity> fetchDocumentFolderList(Long orgId, Integer dataStatus) {
        var scope = DataPermissionScope.current();
        if (scope.userId() == null) {
            return List.of();
        }
        // 无角色范围时只读取共享路径，避免UNION内出现恒假分支触发连接池SQL防护。
        boolean includeManaged = scope.all() || scope.self() || !scope.orgIds().isEmpty();
        return mapper.selectDocumentFolderList(orgId, dataStatus, includeManaged);
    }

    /**
     * 要求目录存在且在当前租户范围中。
     * @param id 目录标识
     * @return 可管理的目录记录
     * @throws ApiRuntimeException 目录不存在时
     */
    public KnowledgeFolderEntity requireWritable(Long id) {
        var query = Wrappers.<KnowledgeFolderEntity>lambdaQuery().eq(KnowledgeFolderEntity::getId, id);
        var entity = mapper.selectOne(query);
        if (entity == null) {
            throw new ApiRuntimeException("目录不存在或无数据权限");
        }
        return entity;
    }

    /**
     * 目录父节点可为空；非空父节点必须属于可访问租户。
     * @param parentId 目标父目录标识，空表示根目录
     * @param orgId 所属部门标识，父子目录必须一致
     */
    public void requireParent(Long parentId, Long orgId) {
        if (parentId != null) {
            requireFolderByOrganization(parentId, orgId);
        }
    }

    /**
     * 按所选部门定位目录，防止目录树跨部门关联，不查询或验证部门本身。
     * @param id 目录标识
     * @param orgId 前端传入的部门标识
     * @return 所选部门中的目录
     */
    public KnowledgeFolderEntity requireFolderByOrganization(Long id, Long orgId) {
        var query = Wrappers.<KnowledgeFolderEntity>lambdaQuery()
                .eq(KnowledgeFolderEntity::getId, id)
                .eq(KnowledgeFolderEntity::getOrgId, orgId);
        var entity = mapper.selectOne(query);
        if (entity == null) {
            throw new ApiRuntimeException("目录不存在、不属于所选部门或无数据权限");
        }
        return entity;
    }

    /**
     * 串行化结构变更，防止并发目录移动形成环，以及删除目录时新增子目录。
     * 调用方必须处于事务内，数据库在事务结束时自动释放锁。
     */
    public void lockStructure() {
        mapper.lockStructure();
    }

    /**
     * 检查移动目标的祖先链，禁止移入自身或后代。
     * @param id 当前目录标识
     * @param targetId 目标父目录，空表示根目录
     * @throws ApiRuntimeException 移动会形成循环时
     */
    public void requireAcyclic(Long id, Long targetId) {
        if (targetId != null && mapper.selectAncestorCount(id, targetId) > 0) {
            throw new ApiRuntimeException("不能移入自身或下级目录");
        }
    }

    /**
     * 创建目录，归属及默认状态由框架填充。
     * @param entity 目录基本信息
     * @return 新建目录标识
     */
    public Long createFolder(KnowledgeFolderEntity entity) {
        // INSERT不追加查询权限条件，新增记录必须主动遵循同一角色数据范围。
        var scope = DataPermissionScope.current();
        if (!scope.canManage(entity.getOrgId(), scope.userId())) {
            throw new ApiRuntimeException("无目录新增数据权限");
        }
        mapper.insert(entity);
        return entity.getId();
    }

    /**
     * 更新目录并通过乐观锁拒绝并发覆盖，父节点允许显式清空。
     * @param entity 本次编辑字段
     * @param version 前端读取的当前版本
     */
    public void modifyFolder(KnowledgeFolderEntity entity, Long version) {
        entity.setVersion(version);
        if (mapper.updateById(entity) != 1) {
            throw new ApiRuntimeException("目录已变化，请刷新后重试");
        }
    }

    /**
     * 删除无子目录占用的目录，检查包括当前用户不可见的记录。
     * @param id 已校验管理权限的目录标识
     */
    public void removeFolder(Long id) {
        if (mapper.selectChildCount(id) > 0) {
            throw new ApiRuntimeException("目录包含子目录，不能删除");
        }
        if (mapper.selectDocumentCount(id) > 0) {
            throw new ApiRuntimeException("目录包含资料，不能删除");
        }
        mapper.deleteById(id);
    }
}
