package org.etd.ai.knowledge.converter;

import org.etd.ai.document.controller.vo.KnowledgeDocumentVO;
import org.etd.ai.document.entity.KnowledgeDocumentEntity;
import org.etd.ai.knowledge.controller.vo.KnowledgeFolderVO;
import org.etd.ai.knowledge.entity.KnowledgeFolderEntity;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.mybaits.permission.model.DataPermissionScope;
import org.springframework.beans.BeanUtils;

/** 目录和文件页面投影，不返回后端存储定位信息。 */
public final class KnowledgePageConverter {
    private KnowledgePageConverter() {}

    /**
     * 输出节点权限，导航祖先仅保留到达子项所需字段。
     *
     * @param e 本次写入的业务记录，租户和审计信息由后端控制
     * @return 通过本方法校验或生成的业务结果
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public static KnowledgeFolderVO toFolderVO(KnowledgeFolderEntity e) {
        var v = new KnowledgeFolderVO();
        BeanUtils.copyProperties(e, v);
        boolean writable = canWrite(e.getOrgId(), e.getCreateBy());
        boolean navigationOnly = !Boolean.TRUE.equals(e.getIsPublic()) && !writable;
        v.setWritable(writable);
        v.setNavigationOnly(navigationOnly);
        if (navigationOnly) {
            v.setVersion(null);
            v.setCreateTime(null);
            v.setUpdateTime(null);
            v.setIsPublic(null);
        }
        return v;
    }

    /**
     * 输出文件基础信息，MinIO对象键和桶名不序列化。
     *
     * @param e 本次写入的业务记录，租户和审计信息由后端控制
     * @return 通过本方法校验或生成的业务结果
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public static KnowledgeDocumentVO toDocumentVO(KnowledgeDocumentEntity e) {
        var v = new KnowledgeDocumentVO();
        BeanUtils.copyProperties(e, v);
        v.setWritable(canWrite(e.getOrgId(), e.getCreateBy()));
        return v;
    }

    private static boolean canWrite(Long orgId, Long creator) {
        return DataPermissionScope.current().canManage(orgId, creator);
    }
}
