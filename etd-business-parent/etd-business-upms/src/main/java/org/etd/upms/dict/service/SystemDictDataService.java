package org.etd.upms.dict.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.etd.upms.dict.controller.dto.SystemDictDataSaveDTO;
import org.etd.upms.dict.controller.vo.SystemDictDataVO;

import java.util.Collection;
import java.util.List;

/**
 * 字典数据项基础能力 Service 接口。
 */
public interface SystemDictDataService {

    IPage<SystemDictDataVO> page(long current, long size, Long dictTypeId, String keyword, Boolean enabled);

    SystemDictDataVO selectById(Long id);

    List<SystemDictDataVO> selectEnabledByTypeId(Long dictTypeId);

    List<SystemDictDataVO> selectEnabledByTypeIds(Collection<Long> dictTypeIds);

    boolean existsByTypeId(Long dictTypeId);

    Long insert(SystemDictDataSaveDTO dto);

    boolean update(Long id, SystemDictDataSaveDTO dto);

    boolean delete(Long id);

    boolean switchEnabled(Long id, Boolean enabled);
}
