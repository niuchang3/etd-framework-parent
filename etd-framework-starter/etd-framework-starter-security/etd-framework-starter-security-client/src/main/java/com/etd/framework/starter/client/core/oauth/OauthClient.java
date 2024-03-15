package com.etd.framework.starter.client.core.oauth;


import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class OauthClient {
    /**
     * 主键ID
     */
    private Long id;
    /**
     * 客户端名称
     */
    private String clientName;

    private String clientId;
    /**
     * 客户端密码
     */
    private String clientPassword;
    /**
     * 客户端授权范围
     */
    private String scopes;
    /**
     * 客户端重定向地址
     */
    private String redirectUri;
    /**
     * 客户端描述
     */
    private String clientDescription;
    /**
     * 客户端Logo
     */
    private String clientLogo;

    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 创建人
     */
    private Long createUser;
    /**
     * 客户端状态
     */
    private String status;


}
