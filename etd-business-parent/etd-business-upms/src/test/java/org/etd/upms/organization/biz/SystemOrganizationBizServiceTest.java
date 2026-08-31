package org.etd.upms.organization.biz;

import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.upms.organization.controller.dto.SystemOrganizationSaveDTO;
import org.etd.upms.organization.controller.vo.SystemOrganizationVO;
import org.etd.upms.organization.service.SystemOrganizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemOrganizationBizServiceTest {

    @Mock
    private SystemOrganizationService organizationService;

    private SystemOrganizationBizService organizationBizService;

    @BeforeEach
    void setUp() {
        organizationBizService = new SystemOrganizationBizService();
        ReflectionTestUtils.setField(organizationBizService, "organizationService", organizationService);
    }

    @Test
    void shouldKeepAncestorWhenChildMatchesKeyword() {
        SystemOrganizationVO root = organization(1L, null, "/", "HEAD", "总部");
        SystemOrganizationVO child = organization(2L, 1L, "/1/", "RD", "研发中心");
        SystemOrganizationVO other = organization(3L, null, "/", "SALE", "销售中心");
        when(organizationService.selectList(true)).thenReturn(List.of(root, child, other));

        List<SystemOrganizationVO> result = organizationBizService.selectTree("研发", true);

        assertThat(result).containsExactly(root);
        assertThat(root.getChildren()).containsExactly(child);
    }

    @Test
    void shouldUpdateDescendantPathsWhenOrganizationMoves() {
        SystemOrganizationVO existing = organization(2L, 1L, "/1/", "RD", "研发中心");
        SystemOrganizationVO newParent = organization(3L, null, "/", "NEW", "新总部");
        SystemOrganizationSaveDTO dto = saveDto(3L);
        when(organizationService.requireExists(2L)).thenReturn(existing);
        when(organizationService.requireExists(3L)).thenReturn(newParent);
        when(organizationService.update(2L, dto, "/3/")).thenReturn(true);

        assertThat(organizationBizService.update(2L, dto)).isTrue();

        verify(organizationService).replaceDescendantPathPrefix("/1/2/", "/3/2/");
    }

    @Test
    void shouldRejectMovingOrganizationBelowItsDescendant() {
        SystemOrganizationVO existing = organization(2L, 1L, "/1/", "RD", "研发中心");
        SystemOrganizationVO descendant = organization(4L, 2L, "/1/2/", "TEAM", "研发一组");
        SystemOrganizationSaveDTO dto = saveDto(4L);
        when(organizationService.requireExists(2L)).thenReturn(existing);
        when(organizationService.requireExists(4L)).thenReturn(descendant);

        assertThatThrownBy(() -> organizationBizService.update(2L, dto))
                .isInstanceOf(ApiRuntimeException.class)
                .hasMessage("上级组织不能选择当前组织的下级组织。");
        verify(organizationService, never()).update(2L, dto, descendant.getParentIdPath());
    }

    private SystemOrganizationVO organization(Long id, Long parentId, String path,
                                                String code, String name) {
        SystemOrganizationVO organization = new SystemOrganizationVO();
        organization.setId(id);
        organization.setParentId(parentId);
        organization.setParentIdPath(path);
        organization.setOrgCode(code);
        organization.setOrgName(name);
        return organization;
    }

    private SystemOrganizationSaveDTO saveDto(Long parentId) {
        SystemOrganizationSaveDTO dto = new SystemOrganizationSaveDTO();
        dto.setParentId(parentId);
        dto.setOrgCode("RD");
        dto.setOrgName("研发中心");
        return dto;
    }
}
