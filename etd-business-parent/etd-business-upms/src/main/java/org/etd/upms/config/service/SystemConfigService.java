package org.etd.upms.config.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.etd.upms.config.controller.dto.SystemConfigSaveDTO;
import org.etd.upms.config.controller.vo.SystemConfigVO;

import java.util.Collection;
import java.util.Map;

/**
 * 系统参数配置能力 Service 接口。
 */
public interface SystemConfigService {

    IPage<SystemConfigVO> page(long current, long size, String keyword, Boolean enabled, String valueType);

    SystemConfigVO selectById(Long id);

    SystemConfigVO selectEnabledByKey(String parameterKey);

    Map<String, String> selectEnabledValuesByKeys(Collection<String> parameterKeys);

    Long insert(SystemConfigSaveDTO dto);

    boolean update(Long id, SystemConfigSaveDTO dto);

    boolean delete(Long id);

    boolean switchEnabled(Long id, Boolean enabled);
}
