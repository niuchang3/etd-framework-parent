package com.etd.framework.starter.client.core.oauth.memory;

import com.etd.framework.starter.client.core.oauth.OauthClient;
import com.etd.framework.starter.client.core.oauth.OauthClientService;
import com.google.common.collect.Maps;

import java.util.Map;

public class OauthClientServiceImpl implements OauthClientService {

    private Map<String, OauthClient> oauthClientMap = Maps.newConcurrentMap();

    @Override
    public OauthClient loadByClientId(String clientId) {
        return oauthClientMap.get(clientId);
    }

    @Override
    public void register(OauthClient client) {
        oauthClientMap.put(client.getClientId(), client);
    }
}
