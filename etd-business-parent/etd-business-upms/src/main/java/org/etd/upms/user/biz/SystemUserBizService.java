package org.etd.upms.user.biz;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.etd.framework.starter.client.core.storage.UserLoginTokenStorage;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.upms.organization.service.SystemOrganizationService;
import org.etd.upms.role.service.SystemRoleService;
import org.etd.upms.user.controller.dto.SystemUserCreateDTO;
import org.etd.upms.user.controller.dto.SystemUserOrganizationAssignDTO;
import org.etd.upms.user.controller.dto.SystemUserRoleAssignDTO;
import org.etd.upms.user.controller.dto.SystemUserUpdateDTO;
import org.etd.upms.user.controller.vo.SystemUserMenusVO;
import org.etd.upms.user.controller.vo.SystemUserOrganizationVO;
import org.etd.upms.user.controller.vo.SystemUserRoleVO;
import org.etd.upms.user.controller.vo.SystemUserVO;
import org.etd.upms.user.entity.SystemUserEntity;
import org.etd.upms.menu.service.SystemMenusService;
import org.etd.upms.role.service.SystemRoleMenuService;
import org.etd.upms.tenant.service.SystemTenantMenuService;
import org.etd.upms.user.service.SystemUserOrganizationService;
import org.etd.upms.user.service.SystemUserRoleRelService;
import org.etd.upms.user.service.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 系统用户业务编排层 BizService，承载用户创建、授权、关系组装等完整业务流程。
 */
@Service
public class SystemUserBizService {

    private static final String UNASSIGNED_DISPLAY = "暂未分配";

    @Autowired
    private SystemUserService userService;

    @Autowired
    private SystemOrganizationService organizationService;

    @Autowired
    private SystemUserOrganizationService userOrganizationService;

    @Autowired
    private SystemRoleService roleService;

    @Autowired
    private SystemMenusService menusService;

    @Autowired
    private SystemTenantMenuService tenantMenuService;

    @Autowired
    private SystemUserRoleRelService userRoleRelService;

    @Autowired
    private SystemRoleMenuService roleMenuService;

    @Autowired
    private UserLoginTokenStorage userLoginTokenStorage;

    public IPage<SystemUserVO> page(long current, long size, String keyword, Long organizationId,
                                    Boolean enabled, Boolean locked) {
        Set<Long> userIds = selectUserIdsByOrganization(organizationId);
        IPage<SystemUserVO> page = userService.page(current, size, keyword, enabled, locked, userIds)
                .convert(this::toVO);
        populateAssignments(page.getRecords());
        return page;
    }

    /**
     * detail
     *
     * @param id 参数 id
     * @return 处理结果
     */
    public SystemUserVO detail(Long id) {
        SystemUserVO user = toVO(userService.requireExists(id));
        populateAssignments(List.of(user));
        return user;
    }

    /**
     * 新增保存
     *
     * @param dto 参数 dto
     * @return 处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Long insert(SystemUserCreateDTO dto) {
        requireTenantId();
        Set<Long> roleIds = normalizedIds(dto.getRoleIds());
        Set<Long> organizationIds = normalizedIds(dto.getOrganizationIds());
        validateAssignments(roleIds, organizationIds, dto.getPrimaryOrganizationId());
        Long userId = userService.insert(toEntity(dto), dto.getPassword());
        userRoleRelService.replace(userId, roleIds);
        userOrganizationService.replace(userId, organizationIds, dto.getPrimaryOrganizationId());
        return userId;
    }

    /**
     * 更新修改
     *
     * @param id 参数 id
     * @param dto 参数 dto
     * @return 处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean update(Long id, SystemUserUpdateDTO dto) {
        userService.requireExists(id);
        if (dto.getRoleIds() != null) {
            requireOrdinaryUser(id, "平台管理员或租户管理员的角色不允许修改。");
            Set<Long> roleIds = normalizedIds(dto.getRoleIds());
            roleService.requireAssignable(roleIds);
            userRoleRelService.replace(id, roleIds);
            revokeUserTokens(id);
        }
        if (dto.getOrganizationIds() != null) {
            Set<Long> organizationIds = normalizedIds(dto.getOrganizationIds());
            validateOrganizations(organizationIds, dto.getPrimaryOrganizationId());
            userOrganizationService.replace(id, organizationIds, dto.getPrimaryOrganizationId());
        }
        return userService.update(id, toEntity(dto));
    }

    /**
     * 删除
     *
     * @param id 参数 id
     * @return 处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        requireOrdinaryUser(id, "平台管理员或租户管理员不允许删除。");
        userRoleRelService.removeByUserId(id);
        userOrganizationService.removeByUserId(id);
        boolean deleted = userService.delete(id);
        if (deleted) {
            revokeUserTokens(id);
        }
        return deleted;
    }

    /**
     * 切换 Enabled
     *
     * @param id 参数 id
     * @param enabled 参数 enabled
     * @return 处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean switchEnabled(Long id, Boolean enabled) {
        if (!enabled) {
            requireOrdinaryUser(id, "平台管理员或租户管理员不允许停用。");
        }
        boolean updated = userService.switchEnabled(id, enabled);
        if (updated && !enabled) {
            revokeUserTokens(id);
        }
        return updated;
    }

    /**
     * 切换 Locked
     *
     * @param id 参数 id
     * @param locked 参数 locked
     * @return 处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean switchLocked(Long id, Boolean locked) {
        if (locked) {
            requireOrdinaryUser(id, "平台管理员或租户管理员不允许锁定。");
        }
        boolean updated = userService.switchLocked(id, locked);
        if (updated && locked) {
            revokeUserTokens(id);
        }
        return updated;
    }

    /**
     * 查询 Roles
     *
     * @param userId 参数 userId
     * @return 处理结果
     */
    public List<SystemUserRoleVO> selectRoles(Long userId) {
        userService.requireExists(userId);
        return userRoleRelService.selectAssignmentsByUserIds(Set.of(userId));
    }

    /**
     * replace Roles
     *
     * @param userId 参数 userId
     * @param dto 参数 dto
     * @return 处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean replaceRoles(Long userId, SystemUserRoleAssignDTO dto) {
        requireOrdinaryUser(userId, "平台管理员或租户管理员的角色不允许修改。");
        Set<Long> roleIds = normalizedIds(dto.getRoleIds());
        roleService.requireAssignable(roleIds);
        userRoleRelService.replace(userId, roleIds);
        revokeUserTokens(userId);
        return true;
    }

    /**
     * 查询 Organizations
     *
     * @param userId 参数 userId
     * @return 处理结果
     */
    public List<SystemUserOrganizationVO> selectOrganizations(Long userId) {
        userService.requireExists(userId);
        return userOrganizationService.selectByUserIds(Set.of(userId));
    }

    /**
     * replace Organizations
     *
     * @param userId 参数 userId
     * @param dto 参数 dto
     * @return 处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean replaceOrganizations(Long userId, SystemUserOrganizationAssignDTO dto) {
        requireOrdinaryUser(userId, "平台管理员或租户管理员的组织不允许修改。");
        Set<Long> organizationIds = normalizedIds(dto.getOrganizationIds());
        validateOrganizations(organizationIds, dto.getPrimaryOrganizationId());
        userOrganizationService.replace(userId, organizationIds, dto.getPrimaryOrganizationId());
        return true;
    }

    /**
     * current User Menus
     *
     * @return 处理结果
     */
    public List<SystemUserMenusVO> currentUserMenus() {
        UserDetails user = RequestContext.getUser();
        Long tenantId = user.getTenantId();
        Set<Long> tenantMenuIds = tenantMenuService.selectMenuIds(tenantId);
        // 管理员直接使用租户菜单，普通用户还需要按角色菜单继续过滤。
        if (user.isAdmin()) {
            return menusService.selectEnabledByIds(tenantMenuIds, tenantId);
        }
        return selectRoleMenus(tenantMenuIds, user.getId(), tenantId);
    }

    private List<SystemUserMenusVO> selectRoleMenus(Set<Long> tenantMenuIds, Long userId, Long tenantId) {
        Set<Long> roleIds = userRoleRelService.selectByUser(userId).stream()
                .map(SystemUserRoleVO::getRoleId)
                .collect(Collectors.toSet());
        Map<Long, String> accessLevels = new LinkedHashMap<>(roleMenuService.selectMenuAccessLevels(roleIds));
        accessLevels.keySet().retainAll(tenantMenuIds);
        List<SystemUserMenusVO> menus = menusService.selectEnabledByIds(accessLevels.keySet(), tenantId);
        menus.forEach(menu -> menu.setAccessLevel(accessLevels.get(menu.getId())));
        return menus;
    }

    private Set<Long> selectUserIdsByOrganization(Long organizationId) {
        if (organizationId == null) {
            return null;
        }
        Set<Long> organizationIds = organizationService.selectSubtreeIds(organizationId);
        return userOrganizationService.selectUserIdsByOrganizationIds(organizationIds);
    }

    private void validateAssignments(Set<Long> roleIds, Set<Long> organizationIds, Long primaryOrganizationId) {
        roleService.requireAssignable(roleIds);
        validateOrganizations(organizationIds, primaryOrganizationId);
    }

    private void validateOrganizations(Set<Long> organizationIds, Long primaryOrganizationId) {
        if (organizationIds.isEmpty() && primaryOrganizationId != null) {
            throw new ApiRuntimeException("未分配组织时不能指定主组织。");
        }
        if (primaryOrganizationId != null && !organizationIds.contains(primaryOrganizationId)) {
            throw new ApiRuntimeException("主组织必须包含在用户组织集合中。");
        }
        organizationService.requireAllExist(organizationIds);
    }

    private void populateAssignments(List<SystemUserVO> users) {
        if (users.isEmpty()) {
            return;
        }
        Set<Long> userIds = users.stream().map(SystemUserVO::getId).collect(Collectors.toSet());
        Map<Long, List<SystemUserRoleVO>> roles = userRoleRelService.selectAssignmentsByUserIds(userIds).stream()
                .collect(Collectors.groupingBy(SystemUserRoleVO::getUserId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<SystemUserOrganizationVO>> organizations = userOrganizationService.selectByUserIds(userIds)
                .stream().collect(Collectors.groupingBy(SystemUserOrganizationVO::getUserId,
                        LinkedHashMap::new, Collectors.toList()));
        users.forEach(user -> populateAssignments(user, roles, organizations));
    }

    private void populateAssignments(SystemUserVO user, Map<Long, List<SystemUserRoleVO>> roleMap,
                                     Map<Long, List<SystemUserOrganizationVO>> organizationMap) {
        List<SystemUserRoleVO> roles = new ArrayList<>(roleMap.getOrDefault(user.getId(), Collections.emptyList()));
        List<SystemUserOrganizationVO> organizations = new ArrayList<>(
                organizationMap.getOrDefault(user.getId(), Collections.emptyList()));
        user.setRoles(roles);
        user.setOrganizations(organizations);
        user.setRoleNames(joinNames(roles.stream().map(SystemUserRoleVO::getRoleName).toList()));
        user.setOrganizationNames(joinNames(organizations.stream()
                .map(SystemUserOrganizationVO::getOrganizationName).toList()));
    }

    private String joinNames(List<String> names) {
        String joined = names.stream().filter(name -> name != null && !name.isBlank())
                .distinct().collect(Collectors.joining("、"));
        return joined.isEmpty() ? UNASSIGNED_DISPLAY : joined;
    }

    private void requireOrdinaryUser(Long userId, String message) {
        userService.requireExists(userId);
        boolean administrator = userRoleRelService.selectByUser(userId).stream()
                .map(SystemUserRoleVO::getRoleCode)
                .anyMatch(this::isAdministratorRole);
        if (administrator) {
            throw new ApiRuntimeException(message);
        }
    }

    private boolean isAdministratorRole(String roleCode) {
        return BasicConstant.SystemRole.PLATFORM_ADMIN.getCode().equalsIgnoreCase(roleCode)
                || BasicConstant.SystemRole.TENANT_ADMIN.getCode().equalsIgnoreCase(roleCode);
    }

    private void revokeUserTokens(Long userId) {
        userLoginTokenStorage.deleteAll(String.valueOf(userId));
    }

    private Long requireTenantId() {
        Long tenantId = RequestContext.getTenantCode();
        if (tenantId == null) {
            throw new ApiRuntimeException("用户维护时必须指定租户。");
        }
        return tenantId;
    }

    private Set<Long> normalizedIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        if (ids.stream().anyMatch(Objects::isNull)) {
            throw new ApiRuntimeException("关联ID不能为空。");
        }
        return new LinkedHashSet<>(ids);
    }

    private SystemUserEntity toEntity(SystemUserCreateDTO dto) {
        SystemUserEntity entity = new SystemUserEntity();
        copyProfile(dto.getAccount(), dto.getUserName(), dto.getMobile(), dto.getBirthday(), dto.getGender(),
                dto.getAvatar(), dto.getNickName(), entity);
        return entity;
    }

    private SystemUserEntity toEntity(SystemUserUpdateDTO dto) {
        SystemUserEntity entity = new SystemUserEntity();
        copyProfile(dto.getAccount(), dto.getUserName(), dto.getMobile(), dto.getBirthday(), dto.getGender(),
                dto.getAvatar(), dto.getNickName(), entity);
        return entity;
    }

    private void copyProfile(String account, String userName, String mobile, java.time.LocalDate birthday,
                             Integer gender,
                             String avatar, String nickName, SystemUserEntity entity) {
        entity.setAccount(account);
        entity.setUserName(userName);
        entity.setMobile(mobile);
        entity.setBirthday(birthday);
        entity.setGender(gender);
        entity.setAvatar(avatar);
        entity.setNickName(nickName);
    }

    private SystemUserVO toVO(SystemUserEntity entity) {
        SystemUserVO vo = new SystemUserVO();
        vo.setId(entity.getId());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        vo.setAccount(entity.getAccount());
        vo.setMobile(entity.getMobile());
        vo.setUserName(entity.getUserName());
        vo.setBirthday(entity.getBirthday());
        vo.setGender(entity.getGender());
        vo.setAvatar(entity.getAvatar());
        vo.setNickName(entity.getNickName());
        vo.setLocked(entity.getLocked());
        vo.setEnabled(entity.getEnabled());
        vo.setDataStatus(entity.getDataStatus());
        return vo;
    }
}
