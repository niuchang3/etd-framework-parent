package com.etd.framework.starter.client.core.token;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Builder
@Data
public class OauthTokenCache implements Serializable {


    private static final long serialVersionUID = 5875711995884719731L;


    private String accessId;

    private String refreshId;

}
