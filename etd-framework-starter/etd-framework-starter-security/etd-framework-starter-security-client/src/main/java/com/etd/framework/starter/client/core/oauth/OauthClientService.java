package com.etd.framework.starter.client.core.oauth;

public interface OauthClientService {

    /**
     * 根据客户端查询
     * @param clientId
     * @return
     */
    OauthClient loadByClientId(String clientId);

    /**
     * 注册客户端
     * @param client
     */
    void register(OauthClient client);


}
