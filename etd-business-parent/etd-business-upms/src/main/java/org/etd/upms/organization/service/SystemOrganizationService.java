package org.etd.upms.organization.service;

import org.etd.upms.organization.controller.dto.SystemOrganizationSaveDTO;
import org.etd.upms.organization.controller.vo.SystemOrganizationVO;

import java.util.List;

public interface SystemOrganizationService {

    List<SystemOrganizationVO> selectList(Boolean enabled);

    SystemOrganizationVO selectById(Long id);

    SystemOrganizationVO requireExists(Long id);

    Long insert(SystemOrganizationSaveDTO dto, String parentIdPath);

    boolean update(Long id, SystemOrganizationSaveDTO dto, String parentIdPath);

    void replaceDescendantPathPrefix(String oldPrefix, String newPrefix);

    boolean existsChild(Long id);

    boolean existsReference(Long id);

    boolean delete(Long id);

    boolean switchEnabled(Long id, Boolean enabled);
}
