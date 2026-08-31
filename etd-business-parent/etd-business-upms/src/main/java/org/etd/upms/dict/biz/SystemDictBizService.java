package org.etd.upms.dict.biz;

import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.upms.dict.controller.dto.SystemDictDataSaveDTO;
import org.etd.upms.dict.controller.vo.SystemDictDataVO;
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
        dictTypeService.requireWritable(dto.getDictTypeId());
        return dictDataService.insert(dto);
    }

    public boolean updateData(Long id, SystemDictDataSaveDTO dto) {
        SystemDictDataVO existing = requireData(id);
        dictTypeService.requireWritable(existing.getDictTypeId());
        dictTypeService.requireWritable(dto.getDictTypeId());
        return dictDataService.update(id, dto);
    }

    public boolean deleteData(Long id) {
        SystemDictDataVO existing = requireData(id);
        dictTypeService.requireWritable(existing.getDictTypeId());
        return dictDataService.delete(id);
    }

    public boolean switchDataEnabled(Long id, Boolean enabled) {
        SystemDictDataVO existing = requireData(id);
        dictTypeService.requireWritable(existing.getDictTypeId());
        return dictDataService.switchEnabled(id, enabled);
    }

    /**
     * 字典类型仍被字典项引用时拒绝删除，避免留下无法管理的孤儿数据。
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteType(Long id) {
        dictTypeService.requireWritable(id);
        if (dictDataService.existsByTypeId(id)) {
            throw new ApiRuntimeException("请先删除该类型下的字典项。");
        }
        return dictTypeService.delete(id);
    }

    private SystemDictDataVO requireData(Long id) {
        SystemDictDataVO data = dictDataService.selectById(id);
        if (data == null) {
            throw new ApiRuntimeException("字典项不存在");
        }
        return data;
    }
}
