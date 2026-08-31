package org.etd.upms.organization.biz;

import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.upms.organization.controller.dto.SystemOrganizationSaveDTO;
import org.etd.upms.organization.controller.vo.SystemOrganizationVO;
import org.etd.upms.organization.service.SystemOrganizationService;
import org.etd.upms.role.service.SystemRoleOrganizationService;
import org.etd.upms.user.service.SystemUserOrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class SystemOrganizationBizService {

    private static final String ROOT_PATH = "/";

    @Autowired
    private SystemOrganizationService organizationService;

    @Autowired
    private SystemUserOrganizationService userOrganizationService;

    @Autowired
    private SystemRoleOrganizationService roleOrganizationService;

    public List<SystemOrganizationVO> selectTree(String keyword, Boolean enabled) {
        List<SystemOrganizationVO> organizations = organizationService.selectList(enabled);
        List<SystemOrganizationVO> roots = buildTree(organizations);
        if (!StringUtils.hasText(keyword)) {
            return roots;
        }
        String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
        roots.removeIf(root -> !retainMatchedBranch(root, normalizedKeyword));
        return roots;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long insert(SystemOrganizationSaveDTO dto) {
        String parentIdPath = resolveParentPath(dto.getParentId(), null);
        return organizationService.insert(dto, parentIdPath);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean update(Long id, SystemOrganizationSaveDTO dto) {
        SystemOrganizationVO existing = organizationService.requireExists(id);
        String newParentPath = resolveParentPath(dto.getParentId(), id);
        boolean updated = organizationService.update(id, dto, newParentPath);
        updateDescendantPaths(id, existing.getParentIdPath(), newParentPath);
        return updated;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        Set<Long> organizationIds = organizationService.selectSubtreeIds(id);
        // 删除组织树时只清理两类组织关系，用户、角色及其他业务数据保留。
        userOrganizationService.removeByOrganizationIds(organizationIds);
        roleOrganizationService.removeByOrganizationIds(organizationIds);
        if (!organizationService.deleteByIds(organizationIds)) {
            throw new ApiRuntimeException("组织架构删除失败。");
        }
        return true;
    }

    private String resolveParentPath(Long parentId, Long currentId) {
        if (parentId == null) {
            return ROOT_PATH;
        }
        if (parentId.equals(currentId)) {
            throw new ApiRuntimeException("上级组织不能选择当前组织。");
        }
        SystemOrganizationVO parent = organizationService.requireExists(parentId);
        String currentPathSegment = ROOT_PATH + currentId + ROOT_PATH;
        if (currentId != null && parent.getParentIdPath().contains(currentPathSegment)) {
            throw new ApiRuntimeException("上级组织不能选择当前组织的下级组织。");
        }
        return parent.getParentIdPath() + parent.getId() + ROOT_PATH;
    }

    private void updateDescendantPaths(Long id, String oldParentPath, String newParentPath) {
        String oldPrefix = oldParentPath + id + ROOT_PATH;
        String newPrefix = newParentPath + id + ROOT_PATH;
        if (!oldPrefix.equals(newPrefix)) {
            // 移动组织时同步修正整棵子树的祖先路径，保证后续层级判断准确。
            organizationService.replaceDescendantPathPrefix(oldPrefix, newPrefix);
        }
    }

    private List<SystemOrganizationVO> buildTree(List<SystemOrganizationVO> organizations) {
        Map<Long, SystemOrganizationVO> organizationsById = new LinkedHashMap<>();
        organizations.forEach(organization -> organizationsById.put(organization.getId(), organization));
        List<SystemOrganizationVO> roots = new ArrayList<>();
        organizations.forEach(organization -> appendToParentOrRoot(organization, organizationsById, roots));
        return roots;
    }

    private void appendToParentOrRoot(SystemOrganizationVO organization,
                                      Map<Long, SystemOrganizationVO> organizationsById,
                                      List<SystemOrganizationVO> roots) {
        SystemOrganizationVO parent = organizationsById.get(organization.getParentId());
        if (parent == null || parent == organization) {
            roots.add(organization);
            return;
        }
        parent.getChildren().add(organization);
    }

    private boolean retainMatchedBranch(SystemOrganizationVO organization, String keyword) {
        organization.getChildren().removeIf(child -> !retainMatchedBranch(child, keyword));
        return organization.getOrgCode().toLowerCase(Locale.ROOT).contains(keyword)
                || organization.getOrgName().toLowerCase(Locale.ROOT).contains(keyword)
                || !organization.getChildren().isEmpty();
    }
}
