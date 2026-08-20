package org.etd.upms.service.role.impl;

import org.etd.upms.mapper.role.SystemRoleMapper;
import org.etd.upms.service.role.SystemRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SystemRoleServiceImpl implements SystemRoleService {

    @Autowired
    private SystemRoleMapper roleMapper;
}
