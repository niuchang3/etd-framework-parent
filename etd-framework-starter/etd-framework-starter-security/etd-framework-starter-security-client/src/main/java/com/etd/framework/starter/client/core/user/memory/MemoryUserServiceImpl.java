package com.etd.framework.starter.client.core.user.memory;

import com.etd.framework.starter.client.core.user.IUserService;
import com.etd.framework.starter.client.core.user.UserDetails;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Objects;

public class MemoryUserServiceImpl implements IUserService {

    private Map<String, UserDetails> userDetailsMap = Maps.newConcurrentMap();


    @Override
    public UserDetails loadUserByAccount(String account) {
        return userDetailsMap.get(account);
    }

    @Override
    public boolean register(UserDetails userDetails) {
        userDetailsMap.put(userDetails.getAccount(), userDetails);
        userDetailsMap.put(userDetails.getId()+"", userDetails);
        return true;
    }

    @Override
    public UserDetails loadUserById(Long id) {
        return userDetailsMap.get(id+"");
    }
}
