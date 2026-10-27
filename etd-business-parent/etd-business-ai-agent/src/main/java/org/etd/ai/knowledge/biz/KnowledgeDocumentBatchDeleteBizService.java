package org.etd.ai.knowledge.biz;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.etd.ai.knowledge.constant.KnowledgePolicy;
import org.etd.ai.knowledge.model.KnowledgeDocumentDeleteFailure;
import org.etd.ai.knowledge.model.KnowledgeDocumentDeleteResult;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.stereotype.Service;

/** 批量删除编排，不持有批次事务；通过另一个Spring代理逐份提交或回滚。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentBatchDeleteBizService {
    private final KnowledgeDocumentBizService documents;

    /**
     * 删除当前知识库内用户明确选择的资料，部分失败时继续处理后续项。
     * @param knowledgeBaseId 当前知识库ID
     * @param documentIds 不重复的资料ID集合，最多100项
     * @return 每份资料的成功或失败结果
     * @throws ApiRuntimeException 批次参数不合法，任何资料均不处理
     */
    public KnowledgeDocumentDeleteResult removeDocumentList(Long knowledgeBaseId, List<Long> documentIds) {
        validateSelection(knowledgeBaseId, documentIds);
        var successIds = new ArrayList<Long>();
        var failures = new ArrayList<KnowledgeDocumentDeleteFailure>();
        for (Long id : documentIds) {
            try {
                // 必须经单条用例的Spring代理进入REQUIRES_NEW，不能自调用绕过事务。
                documents.removeDocumentByBase(id, knowledgeBaseId);
                successIds.add(id);
            } catch (ApiRuntimeException exception) {
                var failure = new KnowledgeDocumentDeleteFailure(id, exception.getMessage());
                failures.add(failure);
            } catch (RuntimeException exception) {
                log.error("批量删除知识资料失败，资料ID={}，异常类型={}", id, exception.getClass().getSimpleName());
                var failure = new KnowledgeDocumentDeleteFailure(id, "删除失败，请刷新后重试");
                failures.add(failure);
            }
        }
        return new KnowledgeDocumentDeleteResult(successIds, failures);
    }

    /** 在任何清理发生前拒绝非法或重复选择，不静默截断或去重。 */
    private void validateSelection(Long knowledgeBaseId, List<Long> ids) {
        if (knowledgeBaseId == null || knowledgeBaseId <= 0 || ids == null || ids.isEmpty()
                || ids.size() > KnowledgePolicy.MAX_BATCH_DELETE) {
            throw new ApiRuntimeException("请选择当前知识库内1至100份资料");
        }
        for (Long id : ids) {
            if (id == null || id <= 0) {
                throw new ApiRuntimeException("资料ID必须为正整数");
            }
        }
        var uniqueIds = new HashSet<>(ids);
        if (uniqueIds.size() != ids.size()) {
            throw new ApiRuntimeException("批量删除的资料不能重复");
        }
    }
}
