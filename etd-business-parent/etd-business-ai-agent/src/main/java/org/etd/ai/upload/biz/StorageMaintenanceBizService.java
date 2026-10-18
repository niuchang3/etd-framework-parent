package org.etd.ai.upload.biz;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.etd.ai.upload.component.MinioMultipartStorage;
import org.etd.ai.upload.mapper.*;
import org.etd.ai.upload.service.StorageCleanupService;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 固定内部维护入口，批次加行锁支持多实例；不改变调用线程租户上下文。 */
@ConditionalOnProperty(
        prefix = "etd.ai.upload",
        name = "maintenance-enabled",
        havingValue = "true",
        matchIfMissing = true)
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageMaintenanceBizService {
    private final UploadSessionMapper sessions;
    private final StorageCleanupMapper queue;
    private final StorageCleanupService cleanups;
    private final MinioMultipartStorage storage;
    private final PlatformTransactionManager transactions;

    /**
     * 过期任务与清理意图原子提交，未完成对象最终由同一队列回收。
     *
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @Scheduled(
            initialDelayString = "${etd.ai.upload.cleanup-interval-ms:60000}",
            fixedDelayString = "${etd.ai.upload.cleanup-interval-ms:60000}")
    public void expireUploadList() {
        new TransactionTemplate(transactions)
                .executeWithoutResult(
                        status -> {
                            for (var s : sessions.selectExpiredSessionList()) {
                                sessions.updateExpiredSession(s.getId());
                                cleanups.createCleanup(
                                        s.getTenantId(),
                                        s.getBucket(),
                                        s.getObjectKey(),
                                        s.getUploadId(),
                                        true);
                            }
                        });
    }

    /**
     * 删除失败保留持久化记录延后重试；日志只包含任务ID，不输出签名URL或凭证。
     *
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @Scheduled(
            initialDelayString = "${etd.ai.upload.cleanup-interval-ms:60000}",
            fixedDelayString = "${etd.ai.upload.cleanup-interval-ms:60000}")
    public void removeOrphanObjectList() {
        new TransactionTemplate(transactions)
                .executeWithoutResult(
                        status -> {
                            for (var e : queue.selectDueCleanupList()) {
                                try {
                                    if (e.getAbortMultipart()) {
                                        storage.abort(
                                                e.getBucket(), e.getObjectKey(), e.getUploadId());
                                    }
                                    storage.remove(e.getBucket(), e.getObjectKey());
                                    queue.deleteCompleted(e.getId());
                                } catch (Exception exception) {
                                    queue.updateRetry(e.getId());
                                    log.warn("AI存储清理失败，将重试，任务ID={}", e.getId());
                                }
                            }
                        });
    }
}
