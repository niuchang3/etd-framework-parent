package org.etd.ai.upload.biz;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;

import lombok.RequiredArgsConstructor;

import org.etd.ai.document.entity.KnowledgeDocumentEntity;
import org.etd.ai.document.service.KnowledgeDocumentService;
import org.etd.ai.knowledge.service.*;
import org.etd.ai.upload.component.*;
import org.etd.ai.upload.constant.UploadState;
import org.etd.ai.upload.constant.MultipartUploadLimits;
import org.etd.ai.upload.entity.UploadSessionEntity;
import org.etd.ai.upload.service.*;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.storage.model.CompleteMultipartReqModel.PartETagInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** 文件传输编排，原件核验通过才登记；此阶段不调用AI模型。 */
@Service
@RequiredArgsConstructor
public class UploadBizService {
    private final UploadSessionService sessions;
    private final MinioMultipartStorage storage;
    private final UploadedFileInspector inspector;
    private final KnowledgeBaseService bases;
    private final KnowledgeFolderService folders;
    private final KnowledgeDocumentService documents;
    private final StorageCleanupService cleanups;

    /**
     * 初始化会话先持久化再调用MinIO，失败或进程中断时可按唯一对象键清理。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param s 上传任务及客户端提交的传输参数，不包含文件类型或大小业务限制
     * @param sessionExpireSeconds 客户端指定的任务有效秒数
     * @return 通过本方法校验或生成的业务结果
     * @throws Exception 对象存储请求或文件内容核验失败时
     */
    public UploadSessionEntity createUpload(
            Long baseId, UploadSessionEntity s, int sessionExpireSeconds) throws Exception {
        bases.requireBase(baseId, false);
        KnowledgeCreationPolicy.requireOrganization();
        folders.requireParent(baseId, s.getFolderId());
        validateReplacement(baseId, s, false);
        populateSession(baseId, s, sessionExpireSeconds);
        sessions.createSession(s);
        String uploadId = storage.initialize(s);
        try {
            sessions.activateSession(s, uploadId);
        } catch (RuntimeException exception) {
            cleanups.createCleanup(
                    s.getTenantId(), s.getBucket(), s.getObjectKey(), uploadId, true);
            throw exception;
        }
        return s;
    }

    /**
     * 签名只允许原用户、有效会话、合法片号及仍然可写的目标资料。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @param number 从1开始的分片序号，不能超过任务总片数
     * @return 指定分片的短期上传URL
     * @throws Exception 对象存储请求或文件内容核验失败时
     */
    public String createPartUrl(Long baseId, Long id, int number) throws Exception {
        var s = sessions.requireSession(baseId, id, false);
        sessions.requireActive(s);
        bases.requireBase(baseId, false);
        folders.requireParent(baseId, s.getFolderId());
        validateReplacement(baseId, s, false);
        if (number < 1 || number > s.getPartCount()) {
            throw new ApiRuntimeException("分片序号超出任务范围");
        }
        long remaining = Duration.between(Instant.now(), s.getExpiresAt()).getSeconds();
        if (remaining < 1) {
            throw new ApiRuntimeException("任务已过期");
        }
        int expirySeconds = (int) Math.min(remaining, s.getUrlExpirySeconds());
        return storage.signPart(s, number, expirySeconds);
    }

    /**
     * 串行完成：锁知识库、会话、旧文件；重复提交返回同一个已登记文档ID。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @param parts 按序提交的完整分片清单，合并前核对存储实际数据
     * @return 业务记录的雪花标识
     * @throws Exception 对象存储请求或文件内容核验失败时
     */
    @Transactional(rollbackFor = Exception.class)
    public Long completeUpload(Long baseId, Long id, List<PartETagInfo> parts) throws Exception {
        bases.requireBase(baseId, true);
        var s = sessions.requireSession(baseId, id, true);
        if (UploadState.COMPLETED.equals(s.getState())) {
            documents.requireReadable(baseId, s.getDocumentId());
            return s.getDocumentId();
        }
        sessions.requireActive(s);
        KnowledgeCreationPolicy.requireOrganization();
        folders.requireParent(baseId, s.getFolderId());
        validateReplacement(baseId, s, true);
        if (!storage.exists(s)) {
            verifyParts(s, parts);
            storage.complete(s, parts);
        }
        UploadedFileInspector.Inspection verified;
        try (var source = storage.open(s)) {
            verified = inspector.inspect(source, s.getFileSize(), s.getOriginalFilename());
        }
        Long documentId = registerDocument(s, verified);
        sessions.completeSession(s, documentId);
        return documentId;
    }

    /**
     * 取消任务只撤销传输，不删除已完成文档；存储清理失败由队列重试。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @Transactional
    public void cancelUpload(Long baseId, Long id) {
        var s = sessions.requireSession(baseId, id, true);
        if (UploadState.COMPLETED.equals(s.getState())
                || UploadState.CANCELLED.equals(s.getState())) {
            return;
        }
        sessions.cancelSession(s);
        cleanups.createCleanup(
                s.getTenantId(), s.getBucket(), s.getObjectKey(), s.getUploadId(), true);
    }

    /** 只检查S3总片数边界，文件类型及业务大小上限由客户端校验。 */
    private void populateSession(Long baseId, UploadSessionEntity s, int sessionExpireSeconds) {
        long partCount = (s.getFileSize() - 1) / s.getPartSizeBytes() + 1;
        if (partCount > MultipartUploadLimits.MAX_PART_COUNT) {
            throw new ApiRuntimeException("分片数量超过S3上限，请增大分片大小");
        }
        s.setId(IdWorker.getId());
        s.setKnowledgeBaseId(baseId);
        s.setBucket(storage.requireBucket());
        s.setObjectKey(RequestContext.getTenantCode() + "/" + baseId + "/" + s.getId());
        s.setPartCount((int) partCount);
        s.setExpiresAt(Instant.now().plusSeconds(sessionExpireSeconds));
    }

    private void validateReplacement(Long baseId, UploadSessionEntity s, boolean lock) {
        if (s.getReplaceDocumentId() == null) {
            if (s.getReplaceVersion() != null) {
                throw new ApiRuntimeException("新文件不能指定替换版本");
            }
            return;
        }
        var old = documents.requireWritable(baseId, s.getReplaceDocumentId(), lock);
        if (s.getReplaceVersion() == null || !old.getVersion().equals(s.getReplaceVersion())) {
            throw new ApiRuntimeException("替换文件版本已变化");
        }
        if (!Objects.equals(old.getFolderId(), s.getFolderId())) {
            throw new ApiRuntimeException("替换上传必须保留原目录");
        }
    }

    private void verifyParts(UploadSessionEntity s, List<PartETagInfo> requested) throws Exception {
        var actual = storage.fetchPartList(s);
        if (actual.size() != s.getPartCount() || requested.size() != actual.size()) {
            throw new ApiRuntimeException("实际分片数量不完整");
        }
        for (int i = 0; i < actual.size(); i++) {
            var stored = actual.get(i);
            var submitted = requested.get(i);
            int number = i + 1;
            long expected =
                    number == s.getPartCount()
                            ? s.getFileSize() - s.getPartSizeBytes() * i
                            : s.getPartSizeBytes();
            if (stored.partNumber() != number
                    || submitted.getPartNumber() != number
                    || stored.partSize() != expected
                    || !stripQuotes(stored.etag()).equals(stripQuotes(submitted.getETag()))) {
                throw new ApiRuntimeException("分片顺序、ETag或实际大小不符合任务");
            }
        }
    }

    private String stripQuotes(String etag) {
        return etag.replace("\"", "");
    }

    private Long registerDocument(
            UploadSessionEntity s, UploadedFileInspector.Inspection verified) {
        if (s.getReplaceDocumentId() == null) {
            var e = new KnowledgeDocumentEntity();
            e.setKnowledgeBaseId(s.getKnowledgeBaseId());
            e.setFolderId(s.getFolderId());
            e.setDocumentName(s.getOriginalFilename());
            populateObject(e, s, verified);
            return documents.createDocument(e);
        }
        var e = documents.requireWritable(s.getKnowledgeBaseId(), s.getReplaceDocumentId(), true);
        cleanups.createCleanup(
                e.getTenantId(), e.getStorageBucket(), e.getStorageObjectKey(), null, false);
        documents.removeProjectionList(e.getId());
        populateObject(e, s, verified);
        documents.replaceDocument(e, s.getReplaceVersion());
        return e.getId();
    }

    private void populateObject(
            KnowledgeDocumentEntity e,
            UploadSessionEntity s,
            UploadedFileInspector.Inspection verified) {
        e.setStorageBucket(s.getBucket());
        e.setStorageObjectKey(s.getObjectKey());
        e.setOriginalFilename(s.getOriginalFilename());
        e.setFileSize(s.getFileSize());
        e.setContentType(verified.contentType());
        e.setFileSha256(verified.sha256());
    }
}
