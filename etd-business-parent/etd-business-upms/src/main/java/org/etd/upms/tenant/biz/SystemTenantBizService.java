package org.etd.upms.tenant.biz;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.upms.tenant.controller.vo.SystemTenantVO;
import org.etd.upms.user.entity.SystemUserEntity;
import org.etd.upms.tenant.service.SystemTenantService;
import org.etd.upms.user.service.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
    private SystemUserService userService;

    public List<SystemTenantVO> selectByUser(UserDetails userDetails) {
        if (ObjectUtils.isEmpty(userDetails)) {
            throw new ApiRuntimeException("该请求需要身份认证。");
        }
        if (ObjectUtils.isEmpty(userDetails.getTenantId())) {
            throw new ApiRuntimeException("登录用户未绑定租户。");
        }
        return tenantService.selectByIds(Set.of(userDetails.getTenantId()));
    }

    public IPage<SystemTenantVO> page(IPage page, List<String> times, String keyword) {
        IPage<SystemTenantVO> tenantPage = tenantService.page(page, times, keyword);
        // 租户分页展示需要补充管理员名称，跨用户能力的组装放在 biz 层。
        populateAdminUser(tenantPage.getRecords());
        return tenantPage;
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
