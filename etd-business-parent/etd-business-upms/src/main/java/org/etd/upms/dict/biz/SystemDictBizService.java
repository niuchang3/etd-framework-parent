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

    /**
     * 新增保存 Data
     *
     * @param dto 参数 dto
     * @return 处理结果
     */
    public Long insertData(SystemDictDataSaveDTO dto) {
        dictTypeService.requireExists(dto.getDictTypeId());
        return dictDataService.insert(dto);
    }

    /**
     * 更新修改 Data
     *
     * @param id 参数 id
     * @param dto 参数 dto
     * @return 处理结果
     */
    public boolean updateData(Long id, SystemDictDataSaveDTO dto) {
        dictTypeService.requireExists(dto.getDictTypeId());
        return dictDataService.update(id, dto);
    }

    /**
     * 删除 Data
     *
     * @param id 参数 id
     * @return 处理结果
     */
    public boolean deleteData(Long id) {
        return dictDataService.delete(id);
    }

    /**
     * 切换 Data Enabled
     *
     * @param id 参数 id
     * @param enabled 参数 enabled
     * @return 处理结果
     */
    public boolean switchDataEnabled(Long id, Boolean enabled) {
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

    private Map<String, List<SystemDictDataVO>> initializeResult(Collection<String> typeCodes) {
        Map<String, List<SystemDictDataVO>> result = new LinkedHashMap<>();
        typeCodes.forEach(typeCode -> result.put(typeCode, List.of()));
        return result;
    }
}
