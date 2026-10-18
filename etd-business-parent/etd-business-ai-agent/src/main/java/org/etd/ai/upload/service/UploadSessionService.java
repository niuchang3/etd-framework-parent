package org.etd.ai.upload.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import lombok.RequiredArgsConstructor;

import org.etd.ai.upload.constant.UploadState;
import org.etd.ai.upload.entity.UploadSessionEntity;
import org.etd.ai.upload.mapper.UploadSessionMapper;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** 持久化上传会话，存储初始化前先提交，使进程中断后仍能清理。 */
@Service
@RequiredArgsConstructor
public class UploadSessionService {
    private final UploadSessionMapper mapper;

    /**
     * 独立事务保存初始化任务，不与随后外部MinIO调用共同回滚。
     *
     * @param s 后端持久化的上传任务及固定策略快照
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createSession(UploadSessionEntity s) {
        mapper.insert(s);
    }

    /**
     * 将真实任务ID登记到已持久化会话。
     *
     * @param s 后端持久化的上传任务及固定策略快照
     * @param uploadId 后端取得的MinIO任务标识，不接受客户端任意指定
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void activateSession(UploadSessionEntity s, String uploadId) {
        var update =
                Wrappers.<UploadSessionEntity>lambdaUpdate()
                        .eq(UploadSessionEntity::getId, s.getId())
                        .eq(UploadSessionEntity::getState, UploadState.INITIALIZING)
                        .set(UploadSessionEntity::getUploadId, uploadId)
                        .set(UploadSessionEntity::getState, UploadState.UPLOADING);
        int count = mapper.update(null, update);
        if (count != 1) {
            throw new ApiRuntimeException("上传初始化已失效");
        }
        s.setUploadId(uploadId);
        s.setState(UploadState.UPLOADING);
    }

    /**
     * 校验任务归属，普通用户只能操作自己的任务；平台管理员可跨用户管理。
     *
     * @param baseId 所属知识库标识，必须位于可信租户范围内
     * @param id 当前操作记录的业务标识，不能绕过租户和访问校验
     * @param lock 是否在调用方事务内获取数据库行锁
     * @return 通过存在性及访问范围校验的业务记录
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public UploadSessionEntity requireSession(Long baseId, Long id, boolean lock) {
        var user = RequestContext.getUser();
        if (user == null || user.getId() == null) {
            throw new ApiRuntimeException("缺少上传任务操作用户");
        }
        var query =
                Wrappers.<UploadSessionEntity>lambdaQuery()
                        .eq(UploadSessionEntity::getId, id)
                        .eq(UploadSessionEntity::getKnowledgeBaseId, baseId)
                        .eq(
                                !user.isPlatformAdmin(),
                                UploadSessionEntity::getCreateBy,
                                user.getId());
        if (lock) {
            query.last("FOR UPDATE");
        }
        var s = mapper.selectOne(query);
        if (s == null) {
            throw new ApiRuntimeException("上传任务不存在或无权限");
        }
        return s;
    }

    /**
     * 只允许未过期、仍在上传的任务签名或合并。
     *
     * @param s 后端持久化的上传任务及固定策略快照
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public void requireActive(UploadSessionEntity s) {
        if (!UploadState.UPLOADING.equals(s.getState())
                || !s.getExpiresAt().isAfter(Instant.now())) {
            throw new ApiRuntimeException("上传任务已取消、过期或不可上传");
        }
    }

    /**
     * 原事务内标记完成并记录文件ID，使重复确认返回同一文件。
     *
     * @param s 后端持久化的上传任务及固定策略快照
     * @param documentId 核验完成后登记的文档标识
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public void completeSession(UploadSessionEntity s, Long documentId) {
        s.setState(UploadState.COMPLETED);
        s.setDocumentId(documentId);
        s.setUpdateTime(null);
        s.setUpdateBy(null);
        if (mapper.updateById(s) != 1) {
            throw new ApiRuntimeException("上传任务已变化，请刷新重试");
        }
    }

    /**
     * 原事务内取消任务，实际存储清理由队列重试。
     *
     * @param s 后端持久化的上传任务及固定策略快照
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public void cancelSession(UploadSessionEntity s) {
        s.setState(UploadState.CANCELLED);
        s.setUpdateTime(null);
        s.setUpdateBy(null);
        if (mapper.updateById(s) != 1) {
            throw new ApiRuntimeException("上传任务已变化，请刷新重试");
        }
    }
}
