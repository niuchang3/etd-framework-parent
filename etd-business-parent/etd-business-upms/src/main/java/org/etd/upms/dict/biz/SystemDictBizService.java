package org.etd.upms.dict.biz;

import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.upms.dict.controller.dto.SystemDictDataSaveDTO;
import org.etd.upms.dict.service.SystemDictDataService;
import org.etd.upms.dict.service.SystemDictTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemDictBizService {

    @Autowired
    private SystemDictTypeService dictTypeService;

    @Autowired
    private SystemDictDataService dictDataService;

    public Long insertData(SystemDictDataSaveDTO dto) {
        dictTypeService.requireExists(dto.getDictTypeId());
        return dictDataService.insert(dto);
    }

    public boolean updateData(Long id, SystemDictDataSaveDTO dto) {
        dictTypeService.requireExists(dto.getDictTypeId());
        return dictDataService.update(id, dto);
    }

    /**
     * 字典类型仍被字典项引用时拒绝删除，避免留下无法管理的孤儿数据。
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteType(Long id) {
        if (dictDataService.existsByTypeId(id)) {
            throw new ApiRuntimeException("请先删除该类型下的字典项。");
        }
        return dictTypeService.delete(id);
    }
}
