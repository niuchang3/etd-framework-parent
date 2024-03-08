package com.etd.framework.starter.oauth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Oauth2Client implements Serializable {

    private static final long serialVersionUID = -1L;
    /**
     * 主键ID
     */
    private Long id;
    /**
     * 客户端名称
     */
    private String clientName;
    /**
     * 客户端 appKey
     */
    private String appKey;
    /**
     * 客户端秘钥证书
     */
    private String appSecret;
    /**
     * 客户端重定向URI
     */
    private String redirectUri;
    /**
     * 权限范围
     */
    private String scopes;
}
