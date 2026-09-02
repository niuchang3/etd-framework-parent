package com.etd.framework.starter.client.core.user.memory;

import com.etd.framework.starter.client.core.user.IUserService;
import org.etd.framework.common.core.user.UserDetails;
import com.google.common.collect.Maps;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * 内存用户服务兜底实现。
 * <p>
 * 业务系统没有提供 {@link IUserService} Bean 时使用，适合本地调试或最小化启动。
 */
public class MemoryUserServiceImpl implements IUserService {

    /**
     * 同时按账号和用户标识保存用户，便于登录与刷新令牌场景查询。
     */
    private final Map<String, UserDetails> userDetailsMap = Maps.newConcurrentMap();


    /**
     * 根据账号加载用户。
     *
     * @param account 登录账号
     * @return 用户信息
     */
    /**
     * load User By Account
     *
     * @param account 参数 account
     * @return 处理结果
     */
    @Override
    public UserDetails loadUserByAccount(String account) {
        return userDetailsMap.get(account);
    }



    /**
     * 根据用户标识加载用户。
     *
     * @param id 用户标识
     * @return 用户信息
     */
    /**
     * load User By Id
     *
     * @param id 参数 id
     * @return 处理结果
     */
    @Override
    public UserDetails loadUserById(Long id) {
        if (id == null) {
            return null;
        }
        return userDetailsMap.get(String.valueOf(id));
    }
}
