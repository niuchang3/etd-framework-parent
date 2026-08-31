package org.etd.upms.user.service;

import java.util.Set;

public interface SystemUserOrganizationService {

    void removeByOrganizationIds(Set<Long> organizationIds);
}
