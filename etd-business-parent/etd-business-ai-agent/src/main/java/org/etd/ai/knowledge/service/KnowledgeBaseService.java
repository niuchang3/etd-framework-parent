package org.etd.ai.knowledge.service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.entity.KnowledgeBaseEntity;
import org.etd.ai.knowledge.mapper.KnowledgeBaseMapper;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.mybaits.permission.model.DataPermissionScope;
import org.springframework.stereotype.Service;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermission;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermissionTable;
/** 知识库基础读写能力；共享读取与维护权限严格分离。 */
// 框架过滤知识库维护资格关联；公开范围仍由SQL业务条件控制，子资源不按上传人重复过滤。
@DataPermission(tables = @DataPermissionTable(table = "ai_knowledge_base", alias = "permission_base"))
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {
    private final KnowledgeBaseMapper mapper;
    /** @param current 页码 @param size 页大小 @param keyword 名称关键字 @return 权限内卡片 */
    public IPage<KnowledgeBaseEntity> fetchBasePage(long current, long size, String keyword) {
        var page = new Page<KnowledgeBaseEntity>(current, size);
        return mapper.selectReadablePage(page, keyword);
    }
    /** @param id 知识库 @param write 是否要求维护资格 @param lock 是否行锁 @return 可访问记录 */
    public KnowledgeBaseEntity requireBase(Long id, boolean write, boolean lock) {
        var base = mapper.selectAccessible(id, lock);
        if (base == null || write && !Boolean.TRUE.equals(base.getWritable())) {
            throw new ApiRuntimeException("知识库不存在或无操作权限");
        }
        return base;
    }
    /** @param entity 已校验配置 @return 新建知识库ID */
    public Long createBase(KnowledgeBaseEntity entity) {
        var scope = DataPermissionScope.current();
        if (!scope.canManage(entity.getOrgId(), scope.userId())) {
            throw new ApiRuntimeException("无知识库新增数据权限");
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
