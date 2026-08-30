package org.etd.upms.role.service.impl;

import org.etd.upms.role.mapper.SystemRoleMapper;
import org.etd.upms.role.service.SystemRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SystemRoleServiceImpl implements SystemRoleService {

    @Autowired
    private SystemRoleMapper roleMapper;
}
