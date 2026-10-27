package org.etd.ai.knowledge.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.constant.KnowledgePolicy;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;
import org.etd.ai.knowledge.mapper.KnowledgeDocumentMapper;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;
import org.etd.framework.starter.mybaits.permission.model.DataPermissionScope;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermission;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermissionTable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.time.Instant;
import org.etd.ai.knowledge.constant.KnowledgeChunkStatus;

/** 资料基础持久化能力；框架按资料自身组织、创建人及公开标记控制访问。 */
// 读取由框架合并本资源公开标记与角色范围；UPDATE、DELETE始终使用严格角色范围。
@DataPermission(tables = @DataPermissionTable(table = "ai_knowledge_document", publicColumn = KnowledgePolicy.PUBLIC_COLUMN))
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentService {
    private final KnowledgeDocumentMapper mapper;

    /** @param current 页码 @param size 每页数量 @param folderId 可选目录
     * @param folderIds 优先筛选的目录集合，非空时忽略 folderId，不自动展开子孙目录
     * @param keyword 可选名称 @param uploadStatus 可选上传状态
     * @param chunkStatus 可选分片状态，未传或空白时不筛选
     * @param knowledgeBaseId 必填知识库 @param dataStatus 可选正式启停
     * @return 数据权限内资料分页 */
    public IPage<KnowledgeDocumentEntity> fetchDocumentPage(long current, long size, Long folderId,
            List<Long> folderIds, String keyword, String uploadStatus, String chunkStatus, Long knowledgeBaseId, Integer dataStatus) {
        var page = new Page<KnowledgeDocumentEntity>(current, size);
        var result = mapper.selectReadablePage(page, knowledgeBaseId, folderId, folderIds, keyword,
                uploadStatus, chunkStatus, dataStatus);
        for (var document : result.getRecords()) {
            populateWritable(document);
        }
        return result;
    }

    /** @param id 资料ID @param lock 是否在当前事务加行锁 @return 可读取资料；lock为true时同时要求严格维护范围
     * @throws ApiRuntimeException 无权限或资料不存在 */
    public KnowledgeDocumentEntity requireDocument(Long id, boolean lock) {
        var entity = mapper.selectAccessible(id, lock);
        if (entity != null) {
            populateWritable(entity);
        }
        if (entity == null || lock && !Boolean.TRUE.equals(entity.getWritable())) {
            throw new ApiRuntimeException("资料不存在或无操作权限");
        }
        return entity;
    }

    /** 资料维护资格由框架统一范围计算，与知识库创建人无关。 */
    private void populateWritable(KnowledgeDocumentEntity document) {
        var scope = DataPermissionScope.current();
        document.setWritable(scope.canManage(document.getOrgId(), document.getCreateBy()));
    }

    /** @param document 已加载资料；有效生成请求期间禁止编辑、删除、合并及重复生成 */
    public void requireChunkIdle(KnowledgeDocumentEntity document) {
        if (KnowledgeChunkStatus.GENERATING.equals(document.getChunkStatus())
                && document.getChunkGenerationExpiresAt() != null
                && document.getChunkGenerationExpiresAt().isAfter(Instant.now())) {
            throw new ApiRuntimeException("资料正在生成分片，请稍后操作");
        }
        if (KnowledgeChunkStatus.GENERATING.equals(document.getChunkStatus())) {
            // 过期请求失去发布资格，后续用户编辑不能再被旧请求的失败状态覆盖。
            document.setChunkGenerationId(null);
            document.setChunkGenerationExpiresAt(null);
            document.setChunkStatus(KnowledgeChunkStatus.FAILED);
            document.setChunkErrorMessage("分片生成请求已超时，可以重新生成");
        }
    }

    /** @param entity 已继承知识库组织的上传主记录 @return 新建资料ID；INSERT主动校验组织维护范围 */
    public Long createDocument(KnowledgeDocumentEntity entity) {
        var scope = DataPermissionScope.current();
        Long orgId = entity.getOrgId();
        // 组织由上传编排从知识库继承，缺失时禁止自动改用上传人的主组织。
        if (orgId == null) {
            throw new ApiRuntimeException("资料缺少知识库组织归属，无法上传");
        }
        if (!scope.canManage(orgId, scope.userId())) {
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
    /**
     * 知识库模型或维度变化后批量调整文档状态；调用方必须先锁定并校验知识库维护权限。
     * @param baseId 已授权知识库
     * @param keyword 是否切换为全文模式
     */
    // 配置失效是知识库维护者触发的派生状态更新，须覆盖该库全部资料，不能只更新本人资料。
    @DataPermission(enable = false)
    public void invalidateVectorStatusByBase(Long baseId, boolean keyword) {
        mapper.updateVectorStatusByBase(baseId, keyword);
    }
    /** @param id 已校验维护权限的资料 @param version 本次确认的processing版本，过期请求不再修改状态 */
    public void markVectorFailure(Long id, Long version) {
        mapper.updateVectorFailure(id, version);
    }
}
