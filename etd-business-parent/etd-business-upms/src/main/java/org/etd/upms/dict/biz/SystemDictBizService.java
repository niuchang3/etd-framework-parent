package org.etd.upms.dict.biz;

import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.upms.dict.controller.dto.SystemDictDataSaveDTO;
import org.etd.upms.dict.controller.vo.SystemDictDataVO;
import org.etd.upms.dict.service.SystemDictDataService;
import org.etd.upms.dict.service.SystemDictTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SystemDictBizService {

    @Autowired
    private SystemDictTypeService dictTypeService;

    @Autowired
    private SystemDictDataService dictDataService;

    /**
     * 批量查询启用的字典类型及字典项，并保留前端传入的编码顺序。
     */
    public Map<String, List<SystemDictDataVO>> selectEnabledDataByTypeCodes(Collection<String> typeCodes) {
        LinkedHashSet<String> distinctTypeCodes = new LinkedHashSet<>(typeCodes);
        Map<String, List<SystemDictDataVO>> result = initializeResult(distinctTypeCodes);
        var types = dictTypeService.selectEnabledByCodes(distinctTypeCodes);
        var typeIds = types.stream().map(type -> type.getId()).toList();
        Map<Long, List<SystemDictDataVO>> dataByTypeId = dictDataService.selectEnabledByTypeIds(typeIds)
                .stream().collect(Collectors.groupingBy(SystemDictDataVO::getDictTypeId));
        types.forEach(type -> result.put(type.getTypeCode(),
                dataByTypeId.getOrDefault(type.getId(), List.of())));
        return result;
    }

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

    private Map<String, List<SystemDictDataVO>> initializeResult(Collection<String> typeCodes) {
        Map<String, List<SystemDictDataVO>> result = new LinkedHashMap<>();
        typeCodes.forEach(typeCode -> result.put(typeCode, List.of()));
        return result;
    }
}
