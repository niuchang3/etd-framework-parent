package org.etd.framework.business.converter;

import com.etd.framework.starter.client.core.user.UserDetails;
import org.etd.framework.business.entity.SystemUserEntity;
import org.etd.framework.business.vo.UserVO;
import org.mapstruct.Mapper;

@Mapper
public interface UserConverter {

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
    UserVO toUserVO(UserDetails userDetails);
}
