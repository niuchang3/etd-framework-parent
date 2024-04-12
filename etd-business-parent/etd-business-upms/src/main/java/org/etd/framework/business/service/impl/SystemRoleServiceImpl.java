package org.etd.framework.business.service.impl;

import org.etd.framework.business.mapper.SystemRoleMapper;
import org.etd.framework.business.service.SystemRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SystemRoleServiceImpl implements SystemRoleService {

    @Autowired
    private SystemRoleMapper roleMapper;
}
