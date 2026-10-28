package org.etd.ai.knowledge.service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.constant.KnowledgePolicy;
import org.etd.ai.knowledge.entity.KnowledgeBaseEntity;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.etd.ai.knowledge.mapper.KnowledgeBaseMapper;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.starter.mybaits.permission.model.DataPermissionScope;
import org.springframework.stereotype.Service;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermission;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermissionTable;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
/** 知识库基础读写能力；共享读取与维护权限严格分离。 */
// 读取由框架合并本资源公开标记与角色范围；UPDATE、DELETE始终使用严格角色范围。
@DataPermission(tables = {@DataPermissionTable(table = "ai_knowledge_base", publicColumn = KnowledgePolicy.PUBLIC_COLUMN),
        @DataPermissionTable(table = "ai_knowledge_document", publicColumn = KnowledgePolicy.PUBLIC_COLUMN)})
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {
    private final KnowledgeBaseMapper mapper;
    /** @param current 页码 @param size 页大小 @param keyword 名称关键字 @return Agent配置可选的启用且可读知识库 */
    public IPage<KnowledgeBaseEntity> fetchAvailableBasePage(long current, long size, String keyword) {
        Page<KnowledgeBaseEntity> page = new Page<KnowledgeBaseEntity>(current, size);
        return mapper.selectAvailablePage(page, keyword, BasicConstant.DataStatus.ENABLED_CODE);
    }

    /** @param current 页码 @param size 页大小 @param keyword 名称关键字 @return 权限内卡片 */
    public IPage<KnowledgeBaseEntity> fetchBasePage(long current, long size, String keyword) {
        Page<KnowledgeBaseEntity> page = new Page<KnowledgeBaseEntity>(current, size);
        IPage<KnowledgeBaseEntity> result = mapper.selectReadablePage(page, keyword);
        for (KnowledgeBaseEntity base : result.getRecords()) {
            populateWritable(base);
        }
        return result;
    }
    /** @param id 知识库 @param write 是否要求维护资格 @param lock 是否行锁 @return 可访问记录 */
    public KnowledgeBaseEntity requireBase(Long id, boolean write, boolean lock) {
        KnowledgeBaseEntity base = mapper.selectAccessible(id, lock);
        if (base != null) {
            populateWritable(base);
        }
        if (base == null || write && !Boolean.TRUE.equals(base.getWritable())) {
            throw new ApiRuntimeException("知识库不存在或无操作权限");
        }
        return base;
    }
    /**
     * 资料维护者读取并锁定索引配置；只供确认用例内部使用，不授予知识库编辑资格。
     * @param document 已通过资料权限查询的记录
     * @return 同租户且仍存在的知识库配置
     * @throws ApiRuntimeException 资料不在维护范围或知识库不存在
     */
    @DataPermission(enable = false)
    public KnowledgeBaseEntity requireIndexBase(KnowledgeDocumentEntity document) {
        requireDocumentManagement(document);
        KnowledgeBaseEntity base = mapper.selectAccessible(document.getKnowledgeBaseId(), true);
        if (base == null) {
            throw new ApiRuntimeException("知识库不存在");
        }
        populateWritable(base);
        return base;
    }

    /**
     * 发布资料索引派生状态，不允许资料维护者修改知识库名称、公开范围或模型设置。
     * @param base 已加锁且配置版本未变化的知识库
     * @param document 已加锁并校验维护资格的资料
     * @throws ApiRuntimeException 归属、维护资格或知识库版本变化
     */
    @DataPermission(enable = false)
    public void activateIndexByDocument(KnowledgeBaseEntity base, KnowledgeDocumentEntity document) {
        requireDocumentManagement(document);
        if (!base.getId().equals(document.getKnowledgeBaseId())) {
            throw new ApiRuntimeException("资料不属于当前知识库");
        }
        long nextVersion = Math.incrementExact(base.getVersion());
        LambdaUpdateWrapper<KnowledgeBaseEntity> update = Wrappers.<KnowledgeBaseEntity>lambdaUpdate()
                .eq(KnowledgeBaseEntity::getId, base.getId())
                .eq(KnowledgeBaseEntity::getVersion, base.getVersion())
                .set(KnowledgeBaseEntity::getActiveIndexVersion, base.getActiveIndexVersion())
                .set(KnowledgeBaseEntity::getActiveConnectionId, base.getActiveConnectionId())
                .set(KnowledgeBaseEntity::getActiveDimensions, base.getActiveDimensions())
                .set(KnowledgeBaseEntity::getVersion, nextVersion);
        if (mapper.update(null, update) != 1) {
            throw new ApiRuntimeException("知识库已变化，请重新确认分片");
        }
        base.setVersion(nextVersion);
    }

    /** 索引内部操作使用资料自身的框架范围，公开读取不构成维护授权。 */
    private void requireDocumentManagement(KnowledgeDocumentEntity document) {
        DataPermissionScope scope = DataPermissionScope.current();
        if (!scope.canManage(document.getOrgId(), document.getCreateBy())) {
            throw new ApiRuntimeException("无资料维护权限");
        }
    }

    /** 页面维护能力复用框架范围计算，公开可读不会授予写入资格。 */
    private void populateWritable(KnowledgeBaseEntity base) {
        DataPermissionScope scope = DataPermissionScope.current();
        base.setWritable(scope.canManage(base.getOrgId(), base.getCreateBy()));
    }
    /** @param entity 已校验配置 @return 新建知识库ID */
    public Long createBase(KnowledgeBaseEntity entity) {
        DataPermissionScope scope = DataPermissionScope.current();
        if (!scope.canManage(entity.getOrgId(), scope.userId())) {
            throw new ApiRuntimeException("无知识库新增数据权限");
        }
        if (entity.getContextWindowSize() == null) {
            entity.setContextWindowSize(0);
        }
        mapper.insert(entity);
        return entity.getId();
    }
    /** @param entity 已锁定配置，索引状态与用户配置统一乐观锁检查 */
    @DataPermission(tables = @DataPermissionTable(table = "ai_knowledge_base"))
    public void modifyBase(KnowledgeBaseEntity entity) {
        entity.setUpdateTime(null);
        entity.setUpdateBy(null);
        if (mapper.updateById(entity) != 1) {
            throw new ApiRuntimeException("知识库已变化，请刷新后重试");
        }
    }
    /** @param id 已授权空知识库；资料占用阻止删除 */
    @DataPermission(tables = @DataPermissionTable(table = "ai_knowledge_base"))
    public void removeBase(Long id) {
        if (mapper.selectDocumentCount(id) > 0) {
            throw new ApiRuntimeException("知识库仍有文档，请先删除文档");
        }
        mapper.deleteById(id);
    }
}
