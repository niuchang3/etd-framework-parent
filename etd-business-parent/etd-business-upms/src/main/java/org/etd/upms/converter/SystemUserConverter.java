package org.etd.upms.converter;

import com.etd.framework.starter.client.core.user.UserDetails;
import org.etd.upms.entity.SystemUserEntity;
import org.etd.upms.controller.user.vo.SystemUserVO;
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
    @Mapping(target = "authorities", ignore = true)
    UserDetails toUserDetails(SystemUserEntity user);

    /**
     * UserDetails 转换UserVo
     * @param userDetails
     * @return
     */
    SystemUserVO toUserVO(UserDetails userDetails);
}
