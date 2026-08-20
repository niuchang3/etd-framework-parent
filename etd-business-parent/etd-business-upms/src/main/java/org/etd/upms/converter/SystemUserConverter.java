package org.etd.upms.converter;

import com.etd.framework.starter.client.core.user.UserDetails;
import org.etd.upms.entity.SystemUserEntity;
import org.etd.upms.controller.user.vo.SystemUserVO;
import org.mapstruct.Mapper;

@Mapper
public interface SystemUserConverter {

    /**
     *
     * @param user
     * @return
     */
    UserDetails toUserDetails(SystemUserEntity user);

    /**
     * UserDetails 转换UserVo
     * @param userDetails
     * @return
     */
    SystemUserVO toUserVO(UserDetails userDetails);
}
