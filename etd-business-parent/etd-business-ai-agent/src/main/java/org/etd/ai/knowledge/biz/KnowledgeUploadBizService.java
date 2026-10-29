package org.etd.ai.knowledge.biz;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.etd.ai.knowledge.config.KnowledgeUploadProperties;
import org.etd.ai.knowledge.constant.KnowledgePolicy;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;
import org.etd.ai.knowledge.model.UploadPart;
import org.etd.ai.knowledge.service.KnowledgeBaseService;
import org.etd.ai.knowledge.service.KnowledgeDocumentService;
import org.etd.ai.knowledge.service.KnowledgeFolderService;
import org.etd.ai.knowledge.service.KnowledgeObjectService;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.constants.BasicConstant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import org.etd.ai.knowledge.entity.KnowledgeBaseEntity;
import org.etd.ai.knowledge.entity.KnowledgeFolderEntity;

/** 分片上传用例；资料行锁串行化会话修改，分片字节流由浏览器直传MinIO。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeUploadBizService {
    private final KnowledgeFolderService folders;
    private final KnowledgeBaseService bases;
    private final KnowledgeDocumentService documents;
    private final KnowledgeObjectService objects;
    private final KnowledgeUploadProperties properties;
    private final TransactionTemplate transactions;

    /** @param document 新上传记录 @return 已初始化的资料；初始化失败清理已知会话 */
    public KnowledgeDocumentEntity createUpload(KnowledgeDocumentEntity document) {
        validateFileSize(document.getFileSize());
        // 展示类型只按原始文件名生成，不依赖浏览器MIME声明，也不随资料显示名称修改。
        String fileType = resolveFileType(document.getOriginalFilename());
        document.setFileType(fileType);
        try {
            return transactions.execute(status -> initializeUpload(document));
        } catch (RuntimeException exception) {
            abortAfterInitializationFailure(document);
            throw exception;
        }
    }

    /** 提取最后一个扩展名作为图标类型；隐藏文件名和末尾空扩展名使用通用图标。 */
    private String resolveFileType(String filename) {
        int separator = filename.lastIndexOf('.');
        if (separator <= 0 || separator == filename.length() - 1) {
            return KnowledgePolicy.UNKNOWN_FILE_TYPE;
        }
        return filename.substring(separator + 1).toLowerCase(Locale.ROOT);
    }

    /** 目录结构锁避免上传初始化与目录删除交叉，INSERT权限由资料服务校验。 */
    private KnowledgeDocumentEntity initializeUpload(KnowledgeDocumentEntity document) {
        populateUploadOwnership(document);
        requireUploadFolder(document);
        populateUploadFields(document);
        documents.createDocument(document);
        try {
            document.setUploadId(objects.initUpload(document));
        } catch (Exception exception) {
            throw storageFailure(document.getId(), "初始化", exception);
        }
        document.setUploadStatus(KnowledgePolicy.UPLOADING);
        documents.modifyDocument(document);
        return document;
    }

    /** 先校验知识库维护权限，再继承其组织和默认公开范围，不采信客户端归属。 */
    private void populateUploadOwnership(KnowledgeDocumentEntity document) {
        KnowledgeBaseEntity base = bases.requireBase(document.getKnowledgeBaseId(), true, false);
        if (base.getDataStatus() != BasicConstant.DataStatus.ENABLED_CODE) {
            throw new ApiRuntimeException("停用知识库不能上传文档");
        }
        // 资料归属已授权的知识库组织，不取上传人的主组织或客户端指定值。
        document.setOrgId(base.getOrgId());
        if (document.getIsPublic() == null) {
            document.setIsPublic(base.getIsPublic());
        }
    }

    /** 在资料入库前持有目录结构锁，校验目录属于当前知识库且处于启用状态。 */
    private void requireUploadFolder(KnowledgeDocumentEntity document) {
        folders.lockStructure();
        KnowledgeFolderEntity folder = folders.requireFolderByBase(document.getFolderId(), document.getKnowledgeBaseId());
        if (folder.getDataStatus() != BasicConstant.DataStatus.ENABLED_CODE) {
            throw new ApiRuntimeException("停用目录不能上传资料");
        }
    }

    /** 初始化原件位置、固定分片参数和会话有效期，仍在原上传事务及目录结构锁内执行。 */
    private void populateUploadFields(KnowledgeDocumentEntity document) {
        document.setId(IdWorker.getId());
        document.setDocumentName(document.getOriginalFilename());
        document.setStorageBucket(objects.requireBucket());
        document.setStorageObjectKey("knowledge/" + document.getId() + "/original");
        document.setPartSizeBytes(properties.getPartSizeBytes());
        // 向上取整得到分片数，避免文件大小恰好为整片时多出空分片。
        long partCount = (document.getFileSize() - 1) / properties.getPartSizeBytes() + 1;
        document.setPartCount(Math.toIntExact(partCount));
        ArrayList<UploadPart> emptyParts = new ArrayList<UploadPart>();
        document.setUploadParts(emptyParts);
        document.setUploadExpiresAt(Instant.now().plusSeconds(properties.getSessionSeconds()));
    }

    /** 上传初始化失败时回收已知会话；网络异常导致ID未知的残片由MinIO生命周期清理。 */
    private void abortAfterInitializationFailure(KnowledgeDocumentEntity document) {
        if (document.getUploadId() != null) {
            try {
                objects.abortUpload(document);
            } catch (Exception exception) {
                log.error("知识资料初始化失败后清理会话失败，资料ID={}，异常类型={}", document.getId(), exception.getClass().getSimpleName());
            }
        }
    }

    /** @param id 资料ID @param partNumber 上传分片编号 @return 临时PUT地址 */
    public String createPartUrl(Long id, int partNumber) {
        return transactions.execute(status -> createLockedPartUrl(id, partNumber));
    }

    /** 调用方事务持有资料行锁，校验会话和分片编号后签发地址。 */
    private String createLockedPartUrl(Long id, int partNumber) {
        KnowledgeDocumentEntity document = documents.requireDocument(id, true);
        requireActiveUpload(document);
        requirePartNumber(document, partNumber);
        try {
            return objects.generatePartUrl(document, partNumber);
        } catch (Exception exception) {
            throw storageFailure(id, "签发上传地址", exception);
        }
    }

    /** @param id 资料ID @param sha256 重新选择文件的摘要 @return 与MinIO核对后的续传记录 */
    public KnowledgeDocumentEntity resumeUpload(Long id, String sha256) {
        return transactions.execute(status -> resumeLockedUpload(id, sha256));
    }

    /** 在原事务和资料行锁内核对文件摘要及远端分片，不提前释放锁。 */
    private KnowledgeDocumentEntity resumeLockedUpload(Long id, String sha256) {
        KnowledgeDocumentEntity document = documents.requireDocument(id, true);
        if (!document.getFileSha256().equals(sha256)) {
            throw new ApiRuntimeException("所选文件与原上传文件不一致");
        }
        if (KnowledgePolicy.COMPLETED.equals(document.getUploadStatus())) {
            return document;
        }
        requireActiveUpload(document);
        try {
            List<UploadPart> actual = objects.fetchPartList(document);
            reconcilePartList(document, actual);
        } catch (Exception exception) {
            throw storageFailure(id, "恢复上传", exception);
        }
        documents.modifyDocument(document);
        return document;
    }

    /** @param id 资料ID @param partNumber 分片编号 @param etag PUT响应标识 @return 更新后的上传进度 */
    public KnowledgeDocumentEntity confirmPart(Long id, int partNumber, String etag) {
        return transactions.execute(status -> confirmLockedPart(id, partNumber, etag));
    }

    /** 核对对象存储回执并在同一资料行锁下合并确认记录，重复确认仍然幂等。 */
    private KnowledgeDocumentEntity confirmLockedPart(Long id, int partNumber, String etag) {
        KnowledgeDocumentEntity document = documents.requireDocument(id, true);
        requireActiveUpload(document);
        requirePartNumber(document, partNumber);
        List<UploadPart> actual;
        try {
            actual = objects.fetchPartList(document);
        } catch (Exception exception) {
            throw storageFailure(id, "核对分片", exception);
        }
        UploadPart part = requireActualPart(actual, partNumber);
        if (!part.etag().equals(etag.replace(String.valueOf('"'), ""))) {
            throw new ApiRuntimeException("分片ETag与对象存储不一致");
        }
        requirePartSize(document, part);
        mergeConfirmedPart(document, part);
        documents.modifyDocument(document);
        return document;
    }

    /** @param id 资料ID @return 完整文件记录；失败状态先提交，再向接口返回错误 */
    public KnowledgeDocumentEntity completeUpload(Long id) {
        KnowledgeDocumentEntity result = transactions.execute(status -> completeLockedUpload(id));
        if (KnowledgePolicy.FAILED.equals(result.getUploadStatus())) {
            throw new ApiRuntimeException(result.getUploadErrorMessage());
        }
        return result;
    }

    /** 同一行锁下完成合并和摘要核验；上次响应丢失时先查询已生成对象。 */
    private KnowledgeDocumentEntity completeLockedUpload(Long id) {
        KnowledgeDocumentEntity document = documents.requireDocument(id, true);
        if (KnowledgePolicy.COMPLETED.equals(document.getUploadStatus())) {
            return document;
        }
        requireActiveUpload(document);
        try {
            if (!objects.existsObject(document)) {
                List<UploadPart> actual = objects.fetchPartList(document);
                reconcilePartList(document, actual);
                requireCompleteParts(document);
            }
            document.setUploadStatus(KnowledgePolicy.COMPLETING);
            documents.modifyDocument(document);
            objects.completeUpload(document);
            objects.verifyObject(document);
            document.setUploadStatus(KnowledgePolicy.COMPLETED);
            document.setUploadedTime(Instant.now());
            document.setUploadErrorMessage(null);
        } catch (Exception exception) {
            logStorageFailure(id, "合并和校验", exception);
            document.setUploadStatus(KnowledgePolicy.FAILED);
            document.setUploadErrorMessage("文件合并或完整性核验失败，可重试；文件不一致时请取消并重新上传");
        }
        documents.modifyDocument(document);
        return document;
    }

    /** @param id 资料ID；重复取消幂等，完整文件不得通过取消接口删除 */
    public void cancelUpload(Long id) {
        transactions.executeWithoutResult(status -> cancelLockedUpload(id));
    }

    /** 取消与状态更新共用原事务和行锁；存储清理失败时仍回滚，不误标记为已取消。 */
    private void cancelLockedUpload(Long id) {
        KnowledgeDocumentEntity document = documents.requireDocument(id, true);
        if (KnowledgePolicy.CANCELLED.equals(document.getUploadStatus())) {
            return;
        }
        if (KnowledgePolicy.COMPLETED.equals(document.getUploadStatus())) {
            throw new ApiRuntimeException("上传已完成，请使用资料删除操作");
        }
        try {
            objects.abortUpload(document);
            objects.removeObject(document);
        } catch (Exception exception) {
            throw storageFailure(id, "取消上传", exception);
        }
        document.setUploadStatus(KnowledgePolicy.CANCELLED);
        documents.modifyDocument(document);
    }

    /** 校验原件大小与S3分片总数技术限制，不读取扩展名系统参数。 */
    private void validateFileSize(long size) {
        long count = (size - 1) / properties.getPartSizeBytes() + 1;
        if (size <= 0 || size > properties.getMaxFileBytes() || count > KnowledgePolicy.MAX_PARTS) {
            throw new ApiRuntimeException("文件大小超过上传限制");
        }
    }

    /** 失败状态允许重试同一会话；取消和过期会话禁止续传。 */
    private void requireActiveUpload(KnowledgeDocumentEntity document) {
        boolean active = KnowledgePolicy.UPLOADING.equals(document.getUploadStatus())
                || KnowledgePolicy.FAILED.equals(document.getUploadStatus());
        if (!active || !document.getUploadExpiresAt().isAfter(Instant.now())) {
            throw new ApiRuntimeException("上传会话已结束或过期，请取消后重新上传");
        }
    }

    /** 上传分片编号必须落在初始化时确定的范围内。 */
    private void requirePartNumber(KnowledgeDocumentEntity document, int number) {
        if (number < 1 || number > document.getPartCount()) {
            throw new ApiRuntimeException("上传分片编号超出范围");
        }
    }

    /** 按固定分片规则核对实际字节数，末片只允许剩余字节。 */
    private void requirePartSize(KnowledgeDocumentEntity document, UploadPart part) {
        requirePartNumber(document, part.partNumber());
        long expected = part.partNumber() == document.getPartCount()
                ? document.getFileSize() - (part.partNumber() - 1L) * document.getPartSizeBytes()
                : document.getPartSizeBytes();
        if (part.size() != expected) {
            throw new ApiRuntimeException("上传分片大小不一致，请重新上传该分片");
        }
    }

    /** 从MinIO清单中定位指定分片，不信任客户端仅声明上传成功。 */
    private UploadPart requireActualPart(List<UploadPart> parts, int number) {
        for (UploadPart part : parts) {
            if (part.partNumber() == number) {
                return part;
            }
        }
        throw new ApiRuntimeException("对象存储尚未找到该上传分片");
    }

    /** 行锁下替换同编号记录，重复确认幂等，避免JSON读改写丢失并发分片。 */
    private void mergeConfirmedPart(KnowledgeDocumentEntity document, UploadPart part) {
        List<UploadPart> parts = new ArrayList<>();
        for (UploadPart confirmed : document.getUploadParts()) {
            if (confirmed.partNumber() != part.partNumber()) {
                parts.add(confirmed);
            }
        }
        parts.add(part);
        parts.sort(Comparator.comparingInt(UploadPart::partNumber));
        document.setUploadParts(parts);
    }

    /** 数据库已确认记录须与远端一致；未确认但已上传的分片供前端重新确认。 */
    private void reconcilePartList(KnowledgeDocumentEntity document, List<UploadPart> actual) {
        ArrayList<UploadPart> confirmed = new ArrayList<UploadPart>();
        for (UploadPart saved : document.getUploadParts()) {
            for (UploadPart remote : actual) {
                if (saved.partNumber() == remote.partNumber() && saved.etag().equals(remote.etag())
                        && saved.size() == remote.size()) {
                    requirePartSize(document, remote);
                    confirmed.add(saved);
                }
            }
        }
        confirmed.sort(Comparator.comparingInt(UploadPart::partNumber));
        document.setUploadParts(confirmed);
    }

    /** 必须确认全部分片，再按升序合并，禁止用远端列表直接替代上传回执。 */
    private void requireCompleteParts(KnowledgeDocumentEntity document) {
        if (document.getUploadParts().size() != document.getPartCount()) {
            throw new ApiRuntimeException("尚有分片未上传或未确认，请恢复上传后重试");
        }
        for (int i = 0; i < document.getPartCount(); i++) {
            if (document.getUploadParts().get(i).partNumber() != i + 1) {
                throw new ApiRuntimeException("上传分片不完整");
            }
        }
    }

    /** 不把SDK异常正文和临时地址返回接口或日志，防止凭证暴露。 */
    private ApiRuntimeException storageFailure(Long id, String action, Exception exception) {
        logStorageFailure(id, action, exception);
        return new ApiRuntimeException("对象存储" + action + "失败，请重试");
    }

    /** 仅记录异常分类与对象存储错误码，不记录响应正文或签名地址。 */
    private void logStorageFailure(Long id, String action, Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String code = cause instanceof ErrorResponseException error ? error.errorResponse().code() : cause.getClass().getSimpleName();
        log.warn("知识资料存储操作失败，资料ID={}，操作={}，错误码={}", id, action, code);
    }
}
