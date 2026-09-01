package org.etd.upms.organization.service;

import org.etd.upms.organization.controller.dto.SystemOrganizationSaveDTO;
import org.etd.upms.organization.controller.vo.SystemOrganizationVO;

import java.util.List;
import java.util.Set;

public interface SystemOrganizationService {

    List<SystemOrganizationVO> selectList(Boolean enabled);

    SystemOrganizationVO selectById(Long id);

    SystemOrganizationVO requireExists(Long id);

    void requireAllExist(Set<Long> ids);

    Long insert(SystemOrganizationSaveDTO dto, String parentIdPath);

    boolean update(Long id, SystemOrganizationSaveDTO dto, String parentIdPath);

    void replaceDescendantPathPrefix(String oldPrefix, String newPrefix);

    boolean existsChild(Long id);

    boolean existsReference(Long id);

    boolean delete(Long id);

    Set<Long> selectSubtreeIds(Long id);

    boolean deleteByIds(Set<Long> ids);

    boolean switchEnabled(Long id, Boolean enabled);
}
