package org.etd.upms.config.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.upms.config.constant.SystemConfigValueType;
import org.etd.upms.config.controller.dto.SystemConfigSaveDTO;
import org.etd.upms.config.controller.vo.SystemConfigVO;
import org.etd.upms.config.entity.SystemConfigEntity;
import org.etd.upms.config.mapper.SystemConfigMapper;
import org.etd.upms.config.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    @Autowired
    private SystemConfigMapper configMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public IPage<SystemConfigVO> page(long current, long size, String keyword, Boolean enabled, String valueType) {
        LambdaQueryWrapper<SystemConfigEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(enabled != null, SystemConfigEntity::getEnabled, enabled)
                .eq(StringUtils.hasText(valueType), SystemConfigEntity::getValueType, valueType)
                .and(StringUtils.hasText(keyword), query -> query
                        .like(SystemConfigEntity::getParameterKey, keyword)
                        .or().like(SystemConfigEntity::getParameterName, keyword))
                .orderByDesc(SystemConfigEntity::getBuiltIn)
                .orderByAsc(SystemConfigEntity::getParameterKey);
        return configMapper.selectPage(new Page<>(current, size), wrapper).convert(this::toVO);
    }

    @Override
    public SystemConfigVO selectById(Long id) {
        return toVO(configMapper.selectById(id));
    }

    @Override
    public SystemConfigVO selectEnabledByKey(String parameterKey) {
        LambdaQueryWrapper<SystemConfigEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfigEntity::getParameterKey, parameterKey)
                .eq(SystemConfigEntity::getEnabled, true);
        return toVO(configMapper.selectOne(wrapper));
    }

    @Override
    public Map<String, String> selectEnabledValuesByKeys(Collection<String> parameterKeys) {
        List<String> distinctKeys = parameterKeys.stream().distinct().toList();
        LambdaQueryWrapper<SystemConfigEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SystemConfigEntity::getParameterKey, distinctKeys)
                .eq(SystemConfigEntity::getEnabled, true);
        Map<String, String> values = configMapper.selectList(wrapper).stream().collect(
                LinkedHashMap::new,
                (result, config) -> result.put(config.getParameterKey(), config.getParameterValue()),
                Map::putAll);
        return orderValues(distinctKeys, values);
    }

    @Override
    public Long insert(SystemConfigSaveDTO dto) {
        validateParameterValue(dto);
        ensureKeyAvailable(dto.getParameterKey(), null);
        SystemConfigEntity entity = toEntity(dto);
        entity.setBuiltIn(false);
        entity.setEnabled(true);
        configMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public boolean update(Long id, SystemConfigSaveDTO dto) {
        SystemConfigEntity existing = requireConfig(id);
        ensureWritable(existing, "内置系统参数不允许修改");
        validateParameterValue(dto);
        ensureKeyAvailable(dto.getParameterKey(), id);
        SystemConfigEntity entity = toEntity(dto);
        entity.setId(id);
        return configMapper.updateById(entity) > 0;
    }

    @Override
    public boolean delete(Long id) {
        SystemConfigEntity entity = requireConfig(id);
        ensureWritable(entity, "内置系统参数不允许删除");
        return configMapper.deleteById(id) > 0;
    }

    @Override
    public boolean switchEnabled(Long id, Boolean enabled) {
        SystemConfigEntity existing = requireConfig(id);
        ensureWritable(existing, "内置系统参数不允许修改启用状态");
        SystemConfigEntity entity = new SystemConfigEntity();
        entity.setId(id);
        entity.setEnabled(enabled);
        return configMapper.updateById(entity) > 0;
    }

    private void ensureKeyAvailable(String parameterKey, Long excludedId) {
        LambdaQueryWrapper<SystemConfigEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfigEntity::getParameterKey, parameterKey)
                .ne(excludedId != null, SystemConfigEntity::getId, excludedId);
        if (configMapper.selectCount(wrapper) > 0) {
            throw new ApiRuntimeException("当前租户下参数键已存在。");
        }
    }

    private SystemConfigEntity requireConfig(Long id) {
        SystemConfigEntity entity = configMapper.selectById(id);
        if (entity == null) {
            throw new ApiRuntimeException("系统参数不存在。");
        }
        return entity;
    }

    private void ensureWritable(SystemConfigEntity entity, String message) {
        if (Boolean.TRUE.equals(entity.getBuiltIn())) {
            throw new ApiRuntimeException(message);
        }
    }

    private Map<String, String> orderValues(List<String> distinctKeys, Map<String, String> values) {
        // 按请求顺序返回，未配置或未启用的参数键不进入结果。
        Map<String, String> orderedValues = new LinkedHashMap<>();
        distinctKeys.forEach(key -> {
            if (values.containsKey(key)) {
                orderedValues.put(key, values.get(key));
            }
        });
        return orderedValues;
    }

    private void validateParameterValue(SystemConfigSaveDTO dto) {
        SystemConfigValueType valueType = SystemConfigValueType.fromCode(dto.getValueType());
        String parameterValue = dto.getParameterValue();
        if (parameterValue == null || valueType == SystemConfigValueType.STRING) {
            return;
        }
        try {
            validateTypedValue(valueType, parameterValue);
        } catch (IllegalArgumentException | JsonProcessingException exception) {
            throw new ApiRuntimeException("参数值与参数值类型不匹配。");
        }
    }

    private void validateTypedValue(SystemConfigValueType valueType, String parameterValue)
            throws JsonProcessingException {
        switch (valueType) {
            case NUMBER -> new BigDecimal(parameterValue);
            case BOOLEAN -> validateBoolean(parameterValue);
            case JSON -> validateJson(parameterValue);
            case STRING -> {
                // 字符串不需要额外格式校验。
            }
        }
    }

    private void validateBoolean(String parameterValue) {
        if (!"true".equalsIgnoreCase(parameterValue) && !"false".equalsIgnoreCase(parameterValue)) {
            throw new IllegalArgumentException("布尔参数只允许true或false");
        }
    }

    private void validateJson(String parameterValue) throws JsonProcessingException {
        JsonNode jsonNode = objectMapper.readTree(parameterValue);
        if (jsonNode == null) {
            throw new IllegalArgumentException("JSON参数不能为空");
        }
    }

    private SystemConfigEntity toEntity(SystemConfigSaveDTO dto) {
        SystemConfigEntity entity = new SystemConfigEntity();
        entity.setParameterKey(dto.getParameterKey());
        entity.setParameterName(dto.getParameterName());
        entity.setParameterValue(dto.getParameterValue());
        entity.setValueType(dto.getValueType());
        entity.setRemark(dto.getRemark());
        return entity;
    }

    private SystemConfigVO toVO(SystemConfigEntity entity) {
        if (entity == null) {
            return null;
        }
        SystemConfigVO vo = new SystemConfigVO();
        vo.setId(entity.getId());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        vo.setParameterKey(entity.getParameterKey());
        vo.setParameterName(entity.getParameterName());
        vo.setParameterValue(entity.getParameterValue());
        vo.setValueType(entity.getValueType());
        vo.setBuiltIn(entity.getBuiltIn());
        vo.setEnabled(entity.getEnabled());
        vo.setRemark(entity.getRemark());
        return vo;
    }
}
