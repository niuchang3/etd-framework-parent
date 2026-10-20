package org.etd.ai.knowledge.biz;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;
import org.etd.ai.knowledge.service.KnowledgeObjectService;
import org.etd.ai.knowledge.service.KnowledgeUploadExpiryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** 根据资料主表取消过期上传；失败记录保留当前状态，后续批次继续尝试。 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "etd.ai.knowledge.upload", name = "cleanup-enabled", havingValue = "true", matchIfMissing = true)
public class KnowledgeUploadExpiryBizService {
    private final KnowledgeUploadExpiryService expiry;
    private final KnowledgeObjectService objects;
    private final TransactionTemplate transactions;
    /** 多实例用skip locked避免重复处理；首次延迟一分钟，启动时不访问外部存储。 */
    @Scheduled(initialDelay = 60000, fixedDelay = 60000)
    public void cancelExpiredUploadList() {
        transactions.executeWithoutResult(status -> {
            for (KnowledgeDocumentEntity document : expiry.fetchExpiredUploadList()) {
                try {
                    objects.abortUpload(document);
                    objects.removeObject(document);
                    expiry.cancelExpiredUpload(document.getId());
                } catch (Exception exception) {
                    log.warn("到期知识上传清理失败，资料ID={}，异常类型={}", document.getId(), exception.getClass().getSimpleName());
                }
            }
        });
    }
}
