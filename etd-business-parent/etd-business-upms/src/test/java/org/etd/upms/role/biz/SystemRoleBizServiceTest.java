package org.etd.upms.role.biz;

import org.etd.framework.common.core.constants.BasicConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.upms.organization.service.SystemOrganizationService;
import org.etd.upms.role.controller.dto.SystemRoleSaveDTO;
import org.etd.upms.role.controller.vo.SystemRoleVO;
import org.etd.upms.role.service.SystemRoleMenuService;
import org.etd.upms.role.service.SystemRoleOrganizationService;
import org.etd.upms.role.service.SystemRoleService;
import org.etd.upms.tenant.service.SystemTenantMenuService;
import org.etd.upms.user.service.SystemUserRoleRelService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemRoleBizServiceTest {

    @Mock
    private SystemRoleService roleService;

    @Mock
    private SystemRoleMenuService roleMenuService;

    @Mock
    private SystemRoleOrganizationService roleOrganizationService;

    @Mock
    private SystemOrganizationService organizationService;

    @Mock
    private SystemTenantMenuService tenantMenuService;

    @Mock
    private SystemUserRoleRelService userRoleRelService;

    @InjectMocks
    private SystemRoleBizService roleBizService;

    @Test
    void shouldInsertCustomOrganizationRelationsWithRole() {
        SystemRoleSaveDTO dto = roleSaveDTO(BasicConstant.PermissionType.CUSTOM_ORGANIZATION, 10L, 20L);
        when(roleService.insert(dto)).thenReturn(100L);
        when(roleOrganizationService.replace(100L, dto.getOrganizationIds())).thenReturn(true);

        Long roleId = roleBizService.insert(dto);

        assertEquals(100L, roleId);
        verify(organizationService).requireAllExist(dto.getOrganizationIds());
        verify(roleOrganizationService).replace(100L, dto.getOrganizationIds());
    }

    @Test
    void shouldClearRelationsWhenPermissionChangesToNonCustom() {
        SystemRoleSaveDTO dto = roleSaveDTO(BasicConstant.PermissionType.ALL, 10L);
        when(roleService.update(100L, dto)).thenReturn(true);
        when(roleOrganizationService.replace(100L, Set.of())).thenReturn(true);

        roleBizService.update(100L, dto);

        verify(organizationService).requireAllExist(Set.of());
        verify(roleOrganizationService).replace(100L, Set.of());
    }

    @Test
    void shouldRejectEmptyOrganizationIdsForCustomPermission() {
        SystemRoleSaveDTO dto = roleSaveDTO(BasicConstant.PermissionType.CUSTOM_ORGANIZATION);

        assertThrows(ApiRuntimeException.class, () -> roleBizService.insert(dto));

        verify(roleService, never()).insert(dto);
    }

    @Test
    void shouldRejectIndependentOrganizationUpdateForNonCustomRole() {
        SystemRoleVO role = new SystemRoleVO();
        role.setPermissionType(BasicConstant.PermissionType.ALL.getCode());
        when(roleService.selectById(100L)).thenReturn(role);

        assertThrows(ApiRuntimeException.class,
                () -> roleBizService.replaceOrganizations(100L, Set.of(10L)));

        verify(roleOrganizationService, never()).replace(100L, Set.of(10L));
    }

    @Test
    void shouldRemoveOrganizationRelationsWhenDeletingRole() {
        when(userRoleRelService.existsByRoleId(100L)).thenReturn(false);
        when(roleService.delete(100L)).thenReturn(true);

        roleBizService.delete(100L);

        verify(roleMenuService).removeByRoleId(100L);
        verify(roleOrganizationService).removeByRoleId(100L);
        verify(roleService).delete(100L);
    }

    private SystemRoleSaveDTO roleSaveDTO(BasicConstant.PermissionType permissionType, Long... organizationIds) {
        SystemRoleSaveDTO dto = new SystemRoleSaveDTO();
        dto.setPermissionType(permissionType.getCode());
        dto.setOrganizationIds(new LinkedHashSet<>(Set.of(organizationIds)));
        return dto;
    }
}
