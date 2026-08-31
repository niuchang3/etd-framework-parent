package org.etd.upms.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.etd.upms.role.entity.SystemRoleOrganizationRelEntity;
import org.etd.upms.role.mapper.SystemRoleOrganizationRelMapper;
import org.etd.upms.role.service.SystemRoleOrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class SystemRoleOrganizationServiceImpl implements SystemRoleOrganizationService {

    @Autowired
    private SystemRoleOrganizationRelMapper roleOrganizationRelMapper;

    @Override
    public void removeByOrganizationIds(Set<Long> organizationIds) {
        if (organizationIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<SystemRoleOrganizationRelEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SystemRoleOrganizationRelEntity::getOrganizationId, organizationIds);
        roleOrganizationRelMapper.delete(wrapper);
    }
}
