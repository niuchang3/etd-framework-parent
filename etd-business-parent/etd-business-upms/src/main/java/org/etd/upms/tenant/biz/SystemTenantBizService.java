package org.etd.upms.tenant.biz;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.etd.framework.starter.client.core.storage.UserLoginTokenStorage;
import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.upms.role.service.SystemRoleService;
import org.etd.upms.tenant.controller.dto.SystemTenantAdminCreateDTO;
import org.etd.upms.tenant.controller.dto.SystemTenantCreateDTO;
import org.etd.upms.tenant.controller.dto.SystemTenantUpdateDTO;
import org.etd.upms.tenant.controller.vo.SystemTenantVO;
import org.etd.upms.tenant.entity.SystemTenantEntity;
import org.etd.upms.user.entity.SystemUserEntity;
import org.etd.upms.tenant.service.SystemTenantService;
import org.etd.upms.user.service.SystemUserRoleRelService;
import org.etd.upms.user.service.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @Transactional(rollbackFor = Exception.class)
    public Long insert(SystemTenantCreateDTO dto) {
        requirePlatformAdmin();
        SystemTenantEntity tenant = toTenantEntity(dto);
        Long tenantId = tenantService.insert(tenant);
        Long adminUserId = createAdminUser(tenantId, dto.getAdministrator());
        Long adminRoleId = roleService.createTenantAdminRole(tenantId, dto.getTenantName());
        userRoleRelService.assignRole(tenantId, adminUserId, adminRoleId);
        if (!tenantService.bindAdminUser(tenantId, adminUserId)) {
            throw new ApiRuntimeException("租户管理员绑定失败。");
        }
        return tenantId;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean update(Long tenantId, SystemTenantUpdateDTO dto) {
        requirePlatformAdmin();
        return tenantService.update(tenantId, toTenantEntity(dto));
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean switchStatus(Long tenantId, Integer status) {
        UserDetails operator = requirePlatformAdmin();
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

    @Transactional(rollbackFor = Exception.class)
    public boolean switchLocked(Long tenantId, boolean locked) {
        UserDetails operator = requirePlatformAdmin();
        if (locked) {
            requireNotCurrentTenant(operator, tenantId, "不能锁定当前登录租户。");
        }
        boolean updated = tenantService.switchLocked(tenantId, locked);
        // TODO(租户安全锁定): 锁定不踢出用户且允许登录；后续权限模块需将该租户的页面操作权限强制降级为只读。
        return updated;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long tenantId) {
        UserDetails operator = requirePlatformAdmin();
        requireNotCurrentTenant(operator, tenantId, "不能删除当前登录租户。");
        boolean deleted = tenantService.delete(tenantId);
        if (deleted) {
            revokeTenantTokens(tenantId);
        }
        return deleted;
    }

    public List<SystemTenantVO> selectByUser(UserDetails userDetails) {
        if (ObjectUtils.isEmpty(userDetails)) {
            throw new ApiRuntimeException("该请求需要身份认证。");
        }
        if (ObjectUtils.isEmpty(userDetails.getTenantId())) {
            throw new ApiRuntimeException("登录用户未绑定租户。");
        }
        return tenantService.selectByIds(Set.of(userDetails.getTenantId()));
    }

    public IPage<SystemTenantVO> page(IPage<SystemTenantEntity> page, List<String> times, String keyword) {
        IPage<SystemTenantVO> tenantPage = tenantService.page(page, times, keyword);
        // 租户分页展示需要补充管理员名称，跨用户能力的组装放在 biz 层。
        populateAdminUser(tenantPage.getRecords());
        return tenantPage;
    }

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

    private void populateAdminUser(SystemTenantVO vo, Map<Long, SystemUserEntity> userMap) {
        if (userMap.containsKey(vo.getTenantAdminUser())) {
            vo.setAdminUser(userMap.get(vo.getTenantAdminUser()).getUserName());
        }
    }

    private Long createAdminUser(Long tenantId, SystemTenantAdminCreateDTO administrator) {
        return userService.createTenantAdmin(tenantId, administrator.getAccount(), administrator.getPassword(),
                administrator.getUserName(), administrator.getMobile());
    }

    private UserDetails requirePlatformAdmin() {
        UserDetails operator = RequestContext.getUser();
        if (operator == null || !operator.isPlatformAdmin()) {
            throw new ApiRuntimeException("只有平台管理员可以维护租户。");
        }
        return operator;
    }

    private void requireNotCurrentTenant(UserDetails operator, Long tenantId, String message) {
        if (Objects.equals(operator.getTenantId(), tenantId)) {
            throw new ApiRuntimeException(message);
        }
    }

    private void revokeTenantTokens(Long tenantId) {
        // 租户不可登录后立即清理存量令牌，避免已登录用户继续访问。
        userService.selectUserIdsByTenantId(tenantId)
                .forEach(userId -> userLoginTokenStorage.deleteAll(String.valueOf(userId)));
    }

    private SystemTenantEntity toTenantEntity(SystemTenantCreateDTO dto) {
        SystemTenantEntity entity = new SystemTenantEntity();
        entity.setLogo(dto.getLogo());
        entity.setTenantName(dto.getTenantName().trim());
        entity.setDescription(dto.getDescription());
        entity.setCreditCode(dto.getCreditCode());
        entity.setTenantType(dto.getTenantType());
        entity.setLocked(false);
        entity.setDataStatus(BasicConstant.DataStatus.ENABLED.getCode());
        return entity;
    }

    private SystemTenantEntity toTenantEntity(SystemTenantUpdateDTO dto) {
        SystemTenantEntity entity = new SystemTenantEntity();
        entity.setLogo(dto.getLogo());
        entity.setTenantName(dto.getTenantName().trim());
        entity.setDescription(dto.getDescription());
        entity.setCreditCode(dto.getCreditCode());
        entity.setTenantType(dto.getTenantType());
        return entity;
    }
}
