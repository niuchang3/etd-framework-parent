package org.etd.upms.role.service;

import java.util.Set;

public interface SystemRoleOrganizationService {

    void removeByOrganizationIds(Set<Long> organizationIds);
}
