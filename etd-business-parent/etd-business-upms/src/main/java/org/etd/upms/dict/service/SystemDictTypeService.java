package org.etd.upms.dict.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.etd.upms.dict.controller.dto.SystemDictTypeSaveDTO;
import org.etd.upms.dict.controller.vo.SystemDictTypeVO;

import java.util.Collection;
import java.util.List;

/**
 * 字典类型基础能力 Service 接口。
 */
public interface SystemDictTypeService {

    IPage<SystemDictTypeVO> page(long current, long size, String keyword, Boolean enabled);

    SystemDictTypeVO selectById(Long id);

    SystemDictTypeVO selectEnabledByCode(String typeCode);

    List<SystemDictTypeVO> selectEnabledByCodes(Collection<String> typeCodes);

    void requireExists(Long id);

    void requireWritable(Long id);

    Long insert(SystemDictTypeSaveDTO dto);

    boolean update(Long id, SystemDictTypeSaveDTO dto);

    boolean delete(Long id);

    boolean switchEnabled(Long id, Boolean enabled);
}
