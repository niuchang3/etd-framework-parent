package org.etd.ai.knowledge.service;

import lombok.RequiredArgsConstructor;
import org.etd.ai.knowledge.entity.KnowledgeDocumentEntity;
import org.etd.ai.knowledge.mapper.KnowledgeDocumentMapper;
import org.springframework.stereotype.Service;
import java.util.List;

/** 仅后台使用的跨租户到期上传访问能力，不提供业务HTTP入口。 */
@Service
@RequiredArgsConstructor
public class KnowledgeUploadExpiryService {
    private final KnowledgeDocumentMapper mapper;
    /** @return 当前事务锁定的到期上传记录 */
    public List<KnowledgeDocumentEntity> fetchExpiredUploadList() {
        return mapper.selectExpiredUploadList();
    }
    /** @param id 本轮已锁定的资料ID */
    public void cancelExpiredUpload(Long id) {
        mapper.updateExpiredUpload(id);
    }
}
