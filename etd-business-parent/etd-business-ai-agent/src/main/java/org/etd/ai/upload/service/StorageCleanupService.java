package org.etd.ai.upload.service;

import lombok.RequiredArgsConstructor;

import org.etd.ai.upload.entity.StorageCleanupEntity;
import org.etd.ai.upload.mapper.StorageCleanupMapper;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;

import java.time.Instant;

/** 业务事务内记录存储清理意图，数据库失败时不误删仍有效原件。 */
@Service
@RequiredArgsConstructor
public class StorageCleanupService {
    private final StorageCleanupMapper mapper;

    /**
     * 为指定可信租户登记清理；调用者在文件替换或删除的同一事务内执行。
     *
     * @param tenantId 后端确定的操作租户标识
     * @param bucket 后端确定的私有存储桶
     * @param objectKey 后端生成且限定租户边界的对象键
     * @param uploadId 后端取得的MinIO任务标识，不接受客户端任意指定
     * @param abort 是否同时中止对象对应的未完成分片任务
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public void createCleanup(
            Long tenantId, String bucket, String objectKey, String uploadId, boolean abort) {
        var e = new StorageCleanupEntity();
        e.setTenantId(tenantId);
        e.setBucket(bucket);
        e.setObjectKey(objectKey);
        e.setUploadId(uploadId);
        e.setAbortMultipart(abort);
        e.setNextAttemptAt(Instant.now());
        mapper.insert(e);
    }
}
