package org.etd.ai.document.biz;

import lombok.RequiredArgsConstructor;

import org.etd.ai.document.entity.KnowledgeDocumentEntity;
import org.etd.ai.document.service.KnowledgeDocumentService;
import org.etd.ai.knowledge.service.*;
import org.etd.ai.upload.component.MinioMultipartStorage;
import org.etd.ai.upload.service.StorageCleanupService;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/** 文件管理编排：公开不授予写权限，删除与清理意图在同一数据库事务提交。 */
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentBizService {
    /** 原件读取签名有效秒数，公开状态修改不撤销已签发链接。 */
    public static final int DOWNLOAD_EXPIRY_SECONDS = 300;

    private final KnowledgeBaseService bases;
    private final KnowledgeFolderService folders;
    private final KnowledgeDocumentService documents;
    private final StorageCleanupService cleanups;
    private final MinioMultipartStorage storage;

    /**
     * 在真实文件读取校验后签发临时链接，不缓存用户的可见范围。
     *
     * @param baseId 所属知识库标识
     * @param id 文档雪花标识
     * @return 仅在有限时间内有效的私有对象读取URL
     * @throws Exception 原件存储签名失败时
     */
    public String createDownloadUrl(Long baseId, Long id) throws Exception {
        var document = documents.requireReadable(baseId, id);
        var base = bases.requireBase(baseId, false);
        requireDownloadEnabled(base.getDataStatus(), document.getDataStatus());
        return storage.signDownload(
                document.getStorageBucket(),
                document.getStorageObjectKey(),
                DOWNLOAD_EXPIRY_SECONDS,
                document.getOriginalFilename());
    }

    /** 管理详情允许查看停用记录，但原件下载继续由后端校验知识库及文件是否启用。 */
    private void requireDownloadEnabled(Integer baseStatus, Integer documentStatus) {
        int enabled = BasicConstant.DataStatus.ENABLED.getCode();
        if (!Objects.equals(baseStatus, enabled) || !Objects.equals(documentStatus, enabled)) {
            throw new ApiRuntimeException("知识库或文件已停用，不能下载原件");
        }
    }

    /**
     * 修改资料和目录，锁定知识库防止目标目录同时被删除。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param e 本次写入的业务记录，租户和审计信息由后端控制
     * @param version 页面读取的当前版本，用于拒绝并发覆盖
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @Transactional
    public void modifyDocument(Long baseId, KnowledgeDocumentEntity e, Long version) {
        bases.requireBase(baseId, true);
        documents.requireWritable(baseId, e.getId(), true);
        folders.requireParent(baseId, e.getFolderId());
        documents.modifyDocument(e, version);
    }

    /**
     * 删除文件先撤下检索数据，原件清理异步重试。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @Transactional
    public void removeDocument(Long baseId, Long id) {
        bases.requireBase(baseId, true);
        var e = documents.requireWritable(baseId, id, true);
        documents.removeProjectionList(id);
        documents.removeDocument(id);
        cleanups.createCleanup(
                e.getTenantId(), e.getStorageBucket(), e.getStorageObjectKey(), null, false);
    }
}
