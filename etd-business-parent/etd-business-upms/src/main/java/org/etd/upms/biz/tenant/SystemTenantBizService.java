package org.etd.upms.biz.tenant;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.etd.framework.starter.client.core.user.UserDetails;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.upms.controller.tenant.vo.SystemTenantVO;
import org.etd.upms.controller.user.vo.SystemUserRoleVO;
import org.etd.upms.entity.SystemUserEntity;
import org.etd.upms.service.tenant.SystemTenantService;
import org.etd.upms.service.user.SystemUserRoleRelService;
import org.etd.upms.service.user.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SystemTenantBizService {

    @Autowired
    private SystemTenantService tenantService;

    @Autowired
    private SystemUserRoleRelService userRoleRelService;

    @Autowired
    private SystemUserService userService;

    public List<SystemTenantVO> selectByUser(UserDetails userDetails) {
        if (ObjectUtils.isEmpty(userDetails)) {
            throw new ApiRuntimeException("该请求需要身份认证。");
        }
        // 平台管理员可以看到全部租户，普通用户只能看到角色关联的租户。
        if (userDetails.isPlatformAdmin()) {
            return tenantService.selectAll();
        }
        return selectTenantByUserRole(userDetails.getId());
    }

    public IPage<SystemTenantVO> page(IPage page, List<String> times, String keyword) {
        IPage<SystemTenantVO> tenantPage = tenantService.page(page, times, keyword);
        // 租户分页展示需要补充管理员名称，跨用户能力的组装放在 biz 层。
        populateAdminUser(tenantPage.getRecords());
        return tenantPage;
    }

    private List<SystemTenantVO> selectTenantByUserRole(Long userId) {
        List<SystemUserRoleVO> roleVOS = userRoleRelService.selectByUser(userId);
        if (CollectionUtils.isEmpty(roleVOS)) {
            throw new ApiRuntimeException("用户身份信息异常,请联系管理员处理");
        }
        Set<Long> tenantIds = roleVOS.stream()
                .map(SystemUserRoleVO::getTenantId)
                .collect(Collectors.toSet());
        return tenantService.selectByIds(tenantIds);
    }

    private void populateAdminUser(List<SystemTenantVO> vos) {
        Set<Long> adminIds = vos.stream()
                .map(SystemTenantVO::getTenantAdminUser)
                .collect(Collectors.toSet());
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
}
