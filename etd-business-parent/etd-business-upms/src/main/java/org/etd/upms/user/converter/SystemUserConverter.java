package org.etd.upms.user.converter;

import org.etd.framework.common.core.user.UserDetails;
import org.etd.upms.user.entity.SystemUserEntity;
import org.etd.upms.user.controller.vo.SystemUserVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface SystemUserConverter {

    /**
     *
     * @param user
     * @return
     */
    @Mapping(target = "platformAdmin", ignore = true)
    @Mapping(target = "tenantAdmin", ignore = true)
    @Mapping(target = "roleCodes", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    UserDetails toUserDetails(SystemUserEntity user);

    /**
     * UserDetails 转换UserVo
     * @param userDetails
     * @return
     */
    SystemUserVO toUserVO(UserDetails userDetails);
}
