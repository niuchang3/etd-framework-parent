package org.etd.ai.knowledge.service;

import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.mybaits.permission.model.DataPermissionScope;

/** 新增目录及上传原件时的组织归属前置条件，不参与查询过滤。 */
public final class KnowledgeCreationPolicy {
    private KnowledgeCreationPolicy() {}

    /**
     * 校验新增记录具备明确租户及主组织，并允许在该组织创建资料。
     *
     * @throws ApiRuntimeException 缺少新增归属信息或没有创建范围时
     */
    public static void requireOrganization() {
        var user = RequestContext.getUser();
        if (user == null
                || user.getId() == null
                || RequestContext.getTenantCode() == null
                || user.getOrgId() == null) {
            throw new ApiRuntimeException("新增资料需要明确的租户及用户主组织");
        }
        if (!DataPermissionScope.current().canManage(user.getOrgId(), user.getId())) {
            throw new ApiRuntimeException("没有资料创建数据权限");
        }
    }
}
