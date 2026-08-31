package org.etd.upms.role.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.etd.upms.role.controller.dto.SystemRoleSaveDTO;
import org.etd.upms.role.controller.vo.SystemRoleVO;

public interface SystemRoleService {

    IPage<SystemRoleVO> page(long current, long size, String keyword, Integer dataStatus);

    SystemRoleVO selectById(Long id);

    void requireExists(Long id);

    void requireWritable(Long id, String message);

    Long insert(SystemRoleSaveDTO dto);

    boolean update(Long id, SystemRoleSaveDTO dto);

    boolean delete(Long id);

    boolean switchStatus(Long id, Integer status);
}
