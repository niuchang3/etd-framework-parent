package org.etd.upms.config.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.etd.upms.config.controller.dto.SystemConfigSaveDTO;
import org.etd.upms.config.controller.vo.SystemConfigVO;

public interface SystemConfigService {

    IPage<SystemConfigVO> page(long current, long size, String keyword, Boolean enabled);

    SystemConfigVO selectById(Long id);

    SystemConfigVO selectEnabledByKey(String parameterKey);

    Long insert(SystemConfigSaveDTO dto);

    boolean update(Long id, SystemConfigSaveDTO dto);

    boolean delete(Long id);

    boolean switchEnabled(Long id, Boolean enabled);
}
