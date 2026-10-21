package org.etd.ai.knowledge.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;
import org.etd.ai.knowledge.mapper.KnowledgeDocumentMapper;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermission;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermissionTable;
import org.etd.framework.starter.mybaits.permission.model.DataPermissionScope;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/** 资料基础持久化能力，统一执行组织数据范围和版本检查。 */
@Service
@RequiredArgsConstructor
@DataPermission(tables = @DataPermissionTable(table = "ai_knowledge_document"))
public class KnowledgeDocumentService {
    private final KnowledgeDocumentMapper mapper;

    /** @param current 页码 @param size 每页数量 @param folderId 可选目录
     * @param folderIds 优先筛选的目录集合，非空时忽略 folderId，不自动展开子孙目录
     * @param keyword 可选名称 @param uploadStatus 可选上传状态 @param processingStatus 可选处理状态
     * @return 数据权限内资料分页 */
    public IPage<KnowledgeDocumentEntity> fetchDocumentPage(long current, long size, Long folderId,
            List<Long> folderIds, String keyword, String uploadStatus, String processingStatus) {
        var query = Wrappers.<KnowledgeDocumentEntity>lambdaQuery()
                .like(StringUtils.hasText(keyword), KnowledgeDocumentEntity::getDocumentName, keyword)
                .eq(StringUtils.hasText(uploadStatus), KnowledgeDocumentEntity::getUploadStatus, uploadStatus)
                .eq(StringUtils.hasText(processingStatus), KnowledgeDocumentEntity::getProcessingStatus, processingStatus)
                .orderByDesc(KnowledgeDocumentEntity::getCreateTime, KnowledgeDocumentEntity::getId);
        // 保留旧单目录调用，批量目录非空时只生成 IN 条件，避免与 folderId 取交集。
        if (!CollectionUtils.isEmpty(folderIds)) {
            query.in(KnowledgeDocumentEntity::getFolderId, folderIds);
        } else if (folderId != null) {
            query.eq(KnowledgeDocumentEntity::getFolderId, folderId);
        }
        var page = new Page<KnowledgeDocumentEntity>(current, size);
        return mapper.selectPage(page, query);
    }

    /** @param id 资料ID @param lock 是否在当前事务加行锁 @return 可维护资料
     * @throws ApiRuntimeException 无权限或资料不存在 */
    public KnowledgeDocumentEntity requireDocument(Long id, boolean lock) {
        var query = Wrappers.<KnowledgeDocumentEntity>lambdaQuery().eq(KnowledgeDocumentEntity::getId, id);
        if (lock) {
            query.last("for update");
        }
        var entity = mapper.selectOne(query);
        if (entity == null) {
            throw new ApiRuntimeException("资料不存在或无数据权限");
        }
        return entity;
    }

    /** @param entity 上传主记录 @return 新建资料ID；INSERT主动校验组织维护范围 */
    public Long createDocument(KnowledgeDocumentEntity entity) {
        var scope = DataPermissionScope.current();
        if (!scope.canManage(entity.getOrgId(), scope.userId())) {
            throw new ApiRuntimeException("无资料新增数据权限");
        }
        mapper.insert(entity);
        return entity.getId();
    }

    /** @param entity 已锁定记录；并发变化时拒绝覆盖 */
    public void modifyDocument(KnowledgeDocumentEntity entity) {
        // 框架只填充空属性；加载后更新时清空旧审计值，交由可信上下文重新填充。
        entity.setUpdateTime(null);
        entity.setUpdateBy(null);
        if (mapper.updateById(entity) != 1) {
            throw new ApiRuntimeException("资料已变化，请刷新后重试");
        }
    }

    /** @param id 已锁定并校验权限的资料ID */
    public void removeDocument(Long id) {
        if (mapper.deleteById(id) != 1) {
            throw new ApiRuntimeException("资料已变化，请刷新后重试");
        }
    }
}
