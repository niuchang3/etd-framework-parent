package org.etd.upms.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.upms.dict.controller.dto.SystemDictDataSaveDTO;
import org.etd.upms.dict.controller.vo.SystemDictDataVO;
import org.etd.upms.dict.entity.SystemDictDataEntity;
import org.etd.upms.dict.mapper.SystemDictDataMapper;
import org.etd.upms.dict.service.SystemDictDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;

@Service
public class SystemDictDataServiceImpl implements SystemDictDataService {

    @Autowired
    private SystemDictDataMapper dictDataMapper;

    @Override
    public IPage<SystemDictDataVO> page(long current, long size, Long dictTypeId,
                                        String keyword, Boolean enabled) {
        LambdaQueryWrapper<SystemDictDataEntity> wrapper = queryWrapper(dictTypeId, keyword, enabled);
        return dictDataMapper.selectPage(new Page<>(current, size), wrapper).convert(this::toVO);
    }

    @Override
    public SystemDictDataVO selectById(Long id) {
        return toVO(dictDataMapper.selectById(id));
    }

    @Override
    public List<SystemDictDataVO> selectEnabledByTypeId(Long dictTypeId) {
        LambdaQueryWrapper<SystemDictDataEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemDictDataEntity::getDictTypeId, dictTypeId)
                .eq(SystemDictDataEntity::getEnabled, true)
                .orderByAsc(SystemDictDataEntity::getSort, SystemDictDataEntity::getId);
        return dictDataMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public List<SystemDictDataVO> selectEnabledByTypeIds(Collection<Long> dictTypeIds) {
        if (dictTypeIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<SystemDictDataEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SystemDictDataEntity::getDictTypeId, dictTypeIds)
                .eq(SystemDictDataEntity::getEnabled, true)
                .orderByAsc(SystemDictDataEntity::getDictTypeId, SystemDictDataEntity::getSort,
                        SystemDictDataEntity::getId);
        return dictDataMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public boolean existsByTypeId(Long dictTypeId) {
        LambdaQueryWrapper<SystemDictDataEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemDictDataEntity::getDictTypeId, dictTypeId);
        return dictDataMapper.selectCount(wrapper) > 0;
    }

    @Override
    public Long insert(SystemDictDataSaveDTO dto) {
        ensureCodeAvailable(dto.getDictTypeId(), dto.getDictCode(), null);
        SystemDictDataEntity entity = toEntity(dto);
        entity.setEnabled(true);
        dictDataMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public boolean update(Long id, SystemDictDataSaveDTO dto) {
        requireData(id);
        ensureCodeAvailable(dto.getDictTypeId(), dto.getDictCode(), id);
        SystemDictDataEntity entity = toEntity(dto);
        entity.setId(id);
        return dictDataMapper.updateById(entity) > 0;
    }

    @Override
    public boolean delete(Long id) {
        requireData(id);
        return dictDataMapper.deleteById(id) > 0;
    }

    @Override
    public boolean switchEnabled(Long id, Boolean enabled) {
        requireData(id);
        SystemDictDataEntity entity = new SystemDictDataEntity();
        entity.setId(id);
        entity.setEnabled(enabled);
        return dictDataMapper.updateById(entity) > 0;
    }

    private LambdaQueryWrapper<SystemDictDataEntity> queryWrapper(Long dictTypeId, String keyword,
                                                                   Boolean enabled) {
        LambdaQueryWrapper<SystemDictDataEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(dictTypeId != null, SystemDictDataEntity::getDictTypeId, dictTypeId)
                .eq(enabled != null, SystemDictDataEntity::getEnabled, enabled)
                .and(StringUtils.hasText(keyword), query -> query
                        .like(SystemDictDataEntity::getDictCode, keyword)
                        .or().like(SystemDictDataEntity::getDictLabel, keyword)
                        .or().like(SystemDictDataEntity::getDictValue, keyword))
                .orderByAsc(SystemDictDataEntity::getSort, SystemDictDataEntity::getId);
        return wrapper;
    }

    private void ensureCodeAvailable(Long typeId, String dictCode, Long excludedId) {
        LambdaQueryWrapper<SystemDictDataEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemDictDataEntity::getDictTypeId, typeId)
                .eq(SystemDictDataEntity::getDictCode, dictCode)
                .ne(excludedId != null, SystemDictDataEntity::getId, excludedId);
        if (dictDataMapper.selectCount(wrapper) > 0) {
            throw new ApiRuntimeException("当前字典类型下字典项编码已存在。");
        }
    }

    private void requireData(Long id) {
        if (dictDataMapper.selectById(id) == null) {
            throw new ApiRuntimeException("字典项不存在。");
        }
    }

    private SystemDictDataEntity toEntity(SystemDictDataSaveDTO dto) {
        SystemDictDataEntity entity = new SystemDictDataEntity();
        entity.setDictTypeId(dto.getDictTypeId());
        entity.setDictCode(dto.getDictCode());
        entity.setDictLabel(dto.getDictLabel());
        entity.setDictValue(dto.getDictValue());
        entity.setSort(dto.getSort());
        entity.setRemark(dto.getRemark());
        return entity;
    }

    private SystemDictDataVO toVO(SystemDictDataEntity entity) {
        if (entity == null) {
            return null;
        }
        SystemDictDataVO vo = new SystemDictDataVO();
        vo.setId(entity.getId());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        vo.setDictTypeId(entity.getDictTypeId());
        vo.setDictCode(entity.getDictCode());
        vo.setDictLabel(entity.getDictLabel());
        vo.setDictValue(entity.getDictValue());
        vo.setSort(entity.getSort());
        vo.setEnabled(entity.getEnabled());
        vo.setRemark(entity.getRemark());
        return vo;
    }
}
