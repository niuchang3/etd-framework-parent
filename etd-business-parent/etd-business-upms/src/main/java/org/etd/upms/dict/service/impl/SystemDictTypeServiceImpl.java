package org.etd.upms.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.upms.dict.controller.dto.SystemDictTypeSaveDTO;
import org.etd.upms.dict.controller.vo.SystemDictTypeVO;
import org.etd.upms.dict.entity.SystemDictTypeEntity;
import org.etd.upms.dict.mapper.SystemDictTypeMapper;
import org.etd.upms.dict.service.SystemDictTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SystemDictTypeServiceImpl implements SystemDictTypeService {

    @Autowired
    private SystemDictTypeMapper dictTypeMapper;

    @Override
    public IPage<SystemDictTypeVO> page(long current, long size, String keyword, Boolean enabled) {
        LambdaQueryWrapper<SystemDictTypeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(enabled != null, SystemDictTypeEntity::getEnabled, enabled)
                .and(StringUtils.hasText(keyword), query -> query
                        .like(SystemDictTypeEntity::getTypeCode, keyword)
                        .or().like(SystemDictTypeEntity::getTypeName, keyword))
                .orderByDesc(SystemDictTypeEntity::getBuiltIn)
                .orderByAsc(SystemDictTypeEntity::getTypeCode);
        return dictTypeMapper.selectPage(new Page<>(current, size), wrapper).convert(this::toVO);
    }

    @Override
    public SystemDictTypeVO selectById(Long id) {
        return toVO(dictTypeMapper.selectById(id));
    }

    @Override
    public SystemDictTypeVO selectEnabledByCode(String typeCode) {
        LambdaQueryWrapper<SystemDictTypeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemDictTypeEntity::getTypeCode, typeCode)
                .eq(SystemDictTypeEntity::getEnabled, true);
        return toVO(dictTypeMapper.selectOne(wrapper));
    }

    @Override
    public void requireExists(Long id) {
        requireType(id);
    }

    @Override
    public void requireWritable(Long id) {
        SystemDictTypeEntity entity = requireType(id);
        if (Boolean.TRUE.equals(entity.getBuiltIn())) {
            throw new ApiRuntimeException("内置字典不允许修改");
        }
    }

    @Override
    public Long insert(SystemDictTypeSaveDTO dto) {
        ensureCodeAvailable(dto.getTypeCode(), null);
        SystemDictTypeEntity entity = toEntity(dto);
        entity.setBuiltIn(false);
        entity.setEnabled(true);
        dictTypeMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public boolean update(Long id, SystemDictTypeSaveDTO dto) {
        requireWritable(id);
        ensureCodeAvailable(dto.getTypeCode(), id);
        SystemDictTypeEntity entity = toEntity(dto);
        entity.setId(id);
        return dictTypeMapper.updateById(entity) > 0;
    }

    @Override
    public boolean delete(Long id) {
        requireWritable(id);
        return dictTypeMapper.deleteById(id) > 0;
    }

    @Override
    public boolean switchEnabled(Long id, Boolean enabled) {
        requireWritable(id);
        SystemDictTypeEntity entity = new SystemDictTypeEntity();
        entity.setId(id);
        entity.setEnabled(enabled);
        return dictTypeMapper.updateById(entity) > 0;
    }

    private void ensureCodeAvailable(String typeCode, Long excludedId) {
        LambdaQueryWrapper<SystemDictTypeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemDictTypeEntity::getTypeCode, typeCode)
                .ne(excludedId != null, SystemDictTypeEntity::getId, excludedId);
        if (dictTypeMapper.selectCount(wrapper) > 0) {
            throw new ApiRuntimeException("字典类型编码已存在。");
        }
    }

    private SystemDictTypeEntity requireType(Long id) {
        SystemDictTypeEntity entity = dictTypeMapper.selectById(id);
        if (entity == null) {
            throw new ApiRuntimeException("字典类型不存在。");
        }
        return entity;
    }

    private SystemDictTypeEntity toEntity(SystemDictTypeSaveDTO dto) {
        SystemDictTypeEntity entity = new SystemDictTypeEntity();
        entity.setTypeCode(dto.getTypeCode());
        entity.setTypeName(dto.getTypeName());
        entity.setRemark(dto.getRemark());
        return entity;
    }

    private SystemDictTypeVO toVO(SystemDictTypeEntity entity) {
        if (entity == null) {
            return null;
        }
        SystemDictTypeVO vo = new SystemDictTypeVO();
        vo.setId(entity.getId());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        vo.setTypeCode(entity.getTypeCode());
        vo.setTypeName(entity.getTypeName());
        vo.setBuiltIn(entity.getBuiltIn());
        vo.setEnabled(entity.getEnabled());
        vo.setRemark(entity.getRemark());
        return vo;
    }
}
