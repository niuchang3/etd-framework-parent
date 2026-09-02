package org.etd.upms.user.service.impl;

import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.user.UserPermissions;
import org.etd.upms.organization.service.SystemOrganizationService;
import org.etd.upms.role.service.SystemRoleOrganizationService;
import org.etd.upms.user.controller.vo.SystemUserOrganizationVO;
import org.etd.upms.user.controller.vo.SystemUserRoleVO;
import org.etd.upms.user.mapper.SystemUserRoleRelMapper;
import org.etd.upms.user.service.SystemUserOrganizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemUserRoleRelServiceImplTest {

    @Mock
    private SystemUserRoleRelMapper userRoleRelMapper;

    @Mock
    private SystemUserOrganizationService userOrganizationService;

    @Mock
    private SystemRoleOrganizationService roleOrganizationService;

    @Mock
    private SystemOrganizationService organizationService;

    @InjectMocks
    private SystemUserRoleRelServiceImpl userRoleRelService;

    @Test
    void shouldMergeSelfSubordinateAndCustomRolePermissions() {
        List<SystemUserRoleVO> roles = List.of(
                role(1L, "SELF_ROLE", BasicConstant.PermissionType.SELF),
                role(2L, "SUB_ROLE", BasicConstant.PermissionType.ORGANIZATION_AND_SUBORDINATE),
                role(3L, "CUSTOM_ROLE", BasicConstant.PermissionType.CUSTOM_ORGANIZATION));
        when(userRoleRelMapper.selectByUserId(100L)).thenReturn(roles);
        when(userOrganizationService.selectByUserIds(Set.of(100L)))
                .thenReturn(List.of(organization(10L, true), organization(11L, false)));
        when(roleOrganizationService.selectOrganizationIdsByRoleIds(Set.of(3L))).thenReturn(Set.of(30L));
        when(organizationService.selectSubtreeIds(10L)).thenReturn(Set.of(10L, 20L));

        UserPermissions permissions = userRoleRelService.loadPermissionsByUser(100L);

        assertEquals(Set.of("SELF_ROLE", "SUB_ROLE", "CUSTOM_ROLE"), permissions.getRoleCodes());
        assertEquals(Set.of("2", "4", "5"), permissions.getPermissionTypes());
        assertEquals(10L, permissions.getPrimaryOrganizationId());
        assertEquals(Set.of(10L, 11L), permissions.getOrganizationIds());
        assertEquals(Set.of(30L), permissions.getCustomOrganizationIds());
        assertEquals(Set.of(10L, 20L, 30L), permissions.getScopeOrganizationIds());
        verify(roleOrganizationService).selectOrganizationIdsByRoleIds(Set.of(3L));
        verify(organizationService).selectSubtreeIds(10L);
    }

    private SystemUserRoleVO role(Long roleId, String roleCode, BasicConstant.PermissionType permissionType) {
        SystemUserRoleVO role = new SystemUserRoleVO();
        role.setTenantId(9L);
        role.setRoleId(roleId);
        role.setRoleCode(roleCode);
        role.setPermissionType(permissionType.getCode());
        return role;
    }

    private SystemUserOrganizationVO organization(Long organizationId, boolean primaryOrganization) {
        SystemUserOrganizationVO organization = new SystemUserOrganizationVO();
        organization.setOrganizationId(organizationId);
        organization.setPrimaryOrganization(primaryOrganization);
        return organization;
    }
}
