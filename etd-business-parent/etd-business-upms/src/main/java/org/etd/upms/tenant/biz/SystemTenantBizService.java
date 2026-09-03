package org.etd.upms.tenant.biz;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.etd.framework.starter.client.core.storage.UserLoginTokenStorage;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.upms.menu.controller.vo.SystemMenuVO;
import org.etd.upms.menu.service.SystemMenusService;
import org.etd.upms.role.service.SystemRoleMenuService;
import org.etd.upms.role.service.SystemRoleService;
import org.etd.upms.tenant.controller.dto.SystemTenantAdminCreateDTO;
import org.etd.upms.tenant.controller.dto.SystemTenantCreateDTO;
import org.etd.upms.tenant.controller.dto.SystemTenantUpdateDTO;
import org.etd.upms.tenant.controller.vo.SystemTenantMenuSettingsVO;
import org.etd.upms.tenant.controller.vo.SystemTenantVO;
import org.etd.upms.tenant.entity.SystemTenantEntity;
import org.etd.upms.tenant.service.SystemTenantMenuService;
import org.etd.upms.tenant.service.SystemTenantService;
import org.etd.upms.user.entity.SystemUserEntity;
import org.etd.upms.user.service.SystemUserRoleRelService;
import org.etd.upms.user.service.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 租户业务编排服务类。
 * <p>
 * 负责组装与编排跨 Service 的完整租户业务流程，包括：
 * 租户创建、管理员初始化、租户菜单授权、状态切换、租户删除及管理员信息组装等。
 */
@Service
public class SystemTenantBizService {

    @Autowired
    private SystemTenantService tenantService;

    @Autowired
    private SystemUserService userService;

    @Autowired
    private SystemRoleService roleService;

    @Autowired
    private SystemUserRoleRelService userRoleRelService;

    @Autowired
    private UserLoginTokenStorage userLoginTokenStorage;

    @Autowired
    private SystemMenusService menusService;

    @Autowired
    private SystemRoleMenuService roleMenuService;

    @Autowired
    private SystemTenantMenuService tenantMenuService;

    /**
     * 新增创建租户及其管理员账号、默认角色与关联关系。
     *
     * @param dto 租户创建请求 DTO 对象
     * @return 创建成功的租户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long insert(SystemTenantCreateDTO dto) {
        SystemTenantEntity tenant = toTenantEntity(dto);
        Long tenantId = tenantService.insert(tenant);
        
        // 初始化创建该租户的初始管理员用户及管理员角色
        Long adminUserId = createAdminUser(tenantId, dto.getAdministrator());
        Long adminRoleId = roleService.createTenantAdminRole(tenantId);
        userRoleRelService.assignRole(tenantId, adminUserId, adminRoleId);
        
        if (!tenantService.bindAdminUser(tenantId, adminUserId)) {
            throw new ApiRuntimeException("租户管理员绑定失败。");
        }
        return tenantId;
    }

    /**
     * 更新指定租户的基础信息。
     *
     * @param tenantId 待更新的租户 ID
     * @param dto      租户更新请求 DTO 对象
     * @return 更新是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean update(Long tenantId, SystemTenantUpdateDTO dto) {
        return tenantService.update(tenantId, toTenantEntity(dto));
    }

    /**
     * 查询指定租户的菜单授权配置数据（全量可选菜单与当前已授权菜单 ID 集合）。
     * <p>
     * 注：租户菜单授权信息属于平台管理能力，仅允许平台管理员操作，普通租户管理员无权读取。
     *
     * @param tenantId 租户 ID
     * @return 租户菜单配置视图对象
     */
    public SystemTenantMenuSettingsVO selectMenuSettings(Long tenantId) {
        tenantService.requireOrdinary(tenantId);
        List<SystemMenuVO> menus = menusService.selectAllEnabled();
        Set<Long> selectedMenuIds = new LinkedHashSet<>(tenantMenuService.selectMenuIds(tenantId));
        // 过滤掉已被停用或不存在的无效菜单 ID
        selectedMenuIds.retainAll(menus.stream().map(SystemMenuVO::getId).collect(Collectors.toSet()));
        
        SystemTenantMenuSettingsVO settings = new SystemTenantMenuSettingsVO();
        settings.setMenus(menus);
        settings.setSelectedMenuIds(selectedMenuIds);
        return settings;
    }

    /**
     * 重置并更新指定租户的可用菜单授权关系。
     * <p>
     * 自动补全选中子菜单依赖的所有父级/祖先菜单节点，并在收回某些菜单时同步清理该租户下角色遗留的对应菜单授权。
     *
     * @param tenantId          租户 ID
     * @param requestedMenuIds 申请授权的目标菜单 ID 集合
     * @return 替换授权是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean replaceMenus(Long tenantId, Set<Long> requestedMenuIds) {
        tenantService.requireOrdinary(tenantId);
        
        // 自动包含选中子节点所需的上级祖先菜单 ID
        Set<Long> normalizedMenuIds = includeAncestorMenus(requestedMenuIds, menusService.selectAllEnabled());
        Set<Long> removedMenuIds = new LinkedHashSet<>(tenantMenuService.selectMenuIds(tenantId));
        removedMenuIds.removeAll(normalizedMenuIds);
        
        // 更新租户菜单关联
        tenantMenuService.replace(tenantId, normalizedMenuIds);
        // 收回租户菜单时清理角色遗留授权，避免之后重新授权租户菜单时意外恢复。
        roleMenuService.removeByTenantAndMenuIds(tenantId, removedMenuIds);
        return true;
    }

    /**
     * 切换租户启用/停用状态。
     * <p>
     * 当租户被停用时，会自动强行清理销毁该租户下所有用户的登录 Token。
     *
     * @param tenantId 租户 ID
     * @param status   目标状态码（启用/停用）
     * @return 状态更新是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean switchStatus(Long tenantId, Integer status) {
        UserDetails operator = RequestContext.getUser();
        boolean disabled = Objects.equals(BasicConstant.DataStatus.DISABLED.getCode(), status);
        if (disabled) {
            requireNotCurrentTenant(operator, tenantId, "不能停用当前登录租户。");
        }
        boolean updated = tenantService.switchStatus(tenantId, status);
        if (updated && disabled) {
            revokeTenantTokens(tenantId);
        }
        return updated;
    }

    /**
     * 切换租户锁定状态。
     *
     * @param tenantId 租户 ID
     * @param locked   是否锁定（true-锁定，false-解锁）
     * @return 锁定状态更新是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean switchLocked(Long tenantId, boolean locked) {
        UserDetails operator = RequestContext.getUser();
        if (locked) {
            requireNotCurrentTenant(operator, tenantId, "不能锁定当前登录租户。");
        }
        boolean updated = tenantService.switchLocked(tenantId, locked);
        // TODO(租户安全锁定): 锁定不踢出用户且允许登录；后续权限模块需将该租户的页面操作权限强制降级为只读。
        return updated;
    }

    /**
     * 删除租户（逻辑删除）。
     * <p>
     * 删除成功后会自动销毁该租户下所有用户的登录 Token。
     *
     * @param tenantId 待删除的租户 ID
     * @return 删除是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long tenantId) {
        UserDetails operator = RequestContext.getUser();
        requireNotCurrentTenant(operator, tenantId, "不能删除当前登录租户。");
        boolean deleted = tenantService.delete(tenantId);
        if (deleted) {
            revokeTenantTokens(tenantId);
        }
        return deleted;
    }

    /**
     * 根据当前登录用户身份查询其所属租户的视图信息列表。
     *
     * @param userDetails 用户身份上下文对象
     * @return 关联租户视图对象列表
     */
    public List<SystemTenantVO> selectTenantListByUser(UserDetails userDetails) {
        if (ObjectUtils.isEmpty(userDetails)) {
            throw new ApiRuntimeException("该请求需要身份认证。");
        }
        if (ObjectUtils.isEmpty(userDetails.getTenantId())) {
            throw new ApiRuntimeException("登录用户未绑定租户。");
        }
        return tenantService.selectByIds(Set.of(userDetails.getTenantId()));
    }

    /**
     * 分页查询租户列表，并组装补全租户管理员的姓名。
     *
     * @param page    分页参数对象
     * @param times   创建时间范围筛选（可选）
     * @param keyword 关键词搜索（可选）
     * @return 补全管理员信息的租户分页 VO 对象
     */
    public IPage<SystemTenantVO> page(IPage<SystemTenantEntity> page, List<Instant> times, String keyword) {
        IPage<SystemTenantVO> tenantPage = tenantService.page(page, times, keyword);
        // 租户分页展示需要补充管理员名称，跨用户能力的组装放在 biz 层。
        populateAdminUser(tenantPage.getRecords());
        return tenantPage;
    }

    /**
     * 批量组装租户列表中管理员账号的姓名信息。
     *
     * @param vos 租户 VO 视图对象列表
     */
    private void populateAdminUser(List<SystemTenantVO> vos) {
        if (vos.isEmpty()) {
            return;
        }
        Set<Long> adminIds = vos.stream()
                .map(SystemTenantVO::getTenantAdminUser)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (adminIds.isEmpty()) {
            return;
        }
        List<SystemUserEntity> users = userService.selectByUserById(adminIds);
        Map<Long, SystemUserEntity> userMap = users.stream()
                .collect(Collectors.toMap(SystemUserEntity::getId, Function.identity()));
        vos.forEach(vo -> populateAdminUser(vo, userMap));
    }

    /**
     * 填充单个租户 VO 视图对象中的管理员姓名。
     *
     * @param vo      租户 VO 视图对象
     * @param userMap 管理员用户对象映射表（key: 用户ID, value: 用户实体）
     */
    private void populateAdminUser(SystemTenantVO vo, Map<Long, SystemUserEntity> userMap) {
        if (userMap.containsKey(vo.getTenantAdminUser())) {
            vo.setAdminUser(userMap.get(vo.getTenantAdminUser()).getUserName());
        }
    }

    /**
     * 创建租户管理员用户实体。
     *
     * @param tenantId      租户 ID
     * @param administrator 管理员信息 DTO
     * @return 创建成功的管理员用户 ID
     */
    private Long createAdminUser(Long tenantId, SystemTenantAdminCreateDTO administrator) {
        return userService.createTenantAdmin(tenantId, administrator.getAccount(), administrator.getPassword(),
                administrator.getUserName(), administrator.getMobile());
    }

    /**
     * 校验操作目标租户不能是当前操作者登录的所属租户（防止自毁）。
     *
     * @param operator 当前操作者信息
     * @param tenantId 操作目标租户 ID
     * @param message  校验不通过时的 ApiRuntimeException 错误提示信息
     */
    private void requireNotCurrentTenant(UserDetails operator, Long tenantId, String message) {
        if (Objects.equals(operator.getTenantId(), tenantId)) {
            throw new ApiRuntimeException(message);
        }
    }

    /**
     * 销毁并清理指定租户下所有用户的登录令牌 Token（强制下线）。
     *
     * @param tenantId 租户 ID
     */
    private void revokeTenantTokens(Long tenantId) {
        // 租户不可登录后立即清理存量令牌，避免已登录用户继续访问。
        userService.selectUserIdsByTenantId(tenantId)
                .forEach(userId -> userLoginTokenStorage.deleteAll(String.valueOf(userId)));
    }

    /**
     * 根据请求授权的菜单集合，递归包含补充所有必要的父级/祖先菜单 ID。
     *
     * @param requestedMenuIds 用户/前端勾选请求的目标菜单 ID 集合
     * @param availableMenus   系统所有已启用的可用菜单视图列表
     * @return 包含完整祖先路径节点后的最终菜单 ID 集合
     */
    private Set<Long> includeAncestorMenus(Set<Long> requestedMenuIds, List<SystemMenuVO> availableMenus) {
        Map<Long, SystemMenuVO> menuMap = availableMenus.stream()
                .collect(Collectors.toMap(SystemMenuVO::getId, Function.identity()));
        if (!menuMap.keySet().containsAll(requestedMenuIds)) {
            throw new ApiRuntimeException("只能为租户授权已启用的菜单。");
        }
        Set<Long> normalizedMenuIds = new LinkedHashSet<>(requestedMenuIds);
        requestedMenuIds.forEach(menuId -> includeAncestors(menuId, menuMap, normalizedMenuIds));
        return normalizedMenuIds;
    }

    /**
     * 递归查找并添加指定菜单节点的所有父级/祖先菜单 ID。
     *
     * @param menuId  当前处理的菜单 ID
     * @param menuMap 菜单映射表（key: 菜单ID, value: 菜单视图）
     * @param menuIds 收集父级菜单 ID 的目标集合
     */
    private void includeAncestors(Long menuId, Map<Long, SystemMenuVO> menuMap, Set<Long> menuIds) {
        Set<Long> visited = new LinkedHashSet<>();
        Long parentId = menuMap.get(menuId).getParentId();
        while (parentId != null && visited.add(parentId)) {
            if (!menuMap.containsKey(parentId)) {
                throw new ApiRuntimeException("菜单的父节点不存在或已停用。");
            }
            menuIds.add(parentId);
            parentId = menuMap.get(parentId).getParentId();
        }
    }

    /**
     * 将租户创建请求 DTO 转换为租户持久化实体对象。
     *
     * @param dto 租户创建请求 DTO
     * @return 租户实体对象
     */
    private SystemTenantEntity toTenantEntity(SystemTenantCreateDTO dto) {
        SystemTenantEntity entity = new SystemTenantEntity();
        entity.setLogo(dto.getLogo());
        entity.setTenantName(dto.getTenantName().trim());
        entity.setDescription(dto.getDescription());
        entity.setCreditCode(dto.getCreditCode());
        // 系统租户只能由初始化数据创建，业务新增统一为普通租户。
        entity.setTenantType(BasicConstant.TenantType.ORDINARY.getCode());
        return entity;
    }

    /**
     * 将租户更新请求 DTO 转换为租户持久化实体对象。
     *
     * @param dto 租户更新请求 DTO
     * @return 租户实体对象
     */
    private SystemTenantEntity toTenantEntity(SystemTenantUpdateDTO dto) {
        SystemTenantEntity entity = new SystemTenantEntity();
        entity.setLogo(dto.getLogo());
        entity.setTenantName(dto.getTenantName().trim());
        entity.setDescription(dto.getDescription());
        entity.setCreditCode(dto.getCreditCode());
        return entity;
    }
}
