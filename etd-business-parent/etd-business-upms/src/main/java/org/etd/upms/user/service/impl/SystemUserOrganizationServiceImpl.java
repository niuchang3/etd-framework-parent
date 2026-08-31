package org.etd.upms.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.etd.upms.user.entity.SystemUserOrganizationRelEntity;
import org.etd.upms.user.mapper.SystemUserOrganizationRelMapper;
import org.etd.upms.user.service.SystemUserOrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class SystemUserOrganizationServiceImpl implements SystemUserOrganizationService {

    @Autowired
    private SystemUserOrganizationRelMapper userOrganizationRelMapper;

    @Override
    public void removeByOrganizationIds(Set<Long> organizationIds) {
        if (organizationIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<SystemUserOrganizationRelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SystemUserOrganizationRelEntity::getOrganizationId, organizationIds);
        userOrganizationRelMapper.delete(wrapper);
    }
}
