package com.etd.framework.starter.oauth.authentication.oauth2.client;

import java.util.Optional;

/**
 * OAuth2客户端仓储。
 */
public interface OAuth2ClientRepository {

    /**
     * 根据客户端主键查询客户端。
     *
     * @param id 客户端主键
     * @return 客户端
     */
    Optional<OAuth2Client> findById(Long id);

    /**
     * 根据客户端ID查询客户端。
     *
     * @param clientId 客户端ID
     * @return 客户端
     */
    Optional<OAuth2Client> findByClientId(String clientId);
}
