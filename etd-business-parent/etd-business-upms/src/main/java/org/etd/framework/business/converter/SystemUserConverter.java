package org.etd.framework.business.converter;

import com.etd.framework.starter.client.core.user.UserDetails;
import org.etd.framework.business.entity.SystemUserEntity;
import org.etd.framework.business.vo.SystemUserVO;
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
