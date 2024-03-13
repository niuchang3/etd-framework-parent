package com.etd.framework.starter.client.core.token;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class OauthTokenValue {

    @JSONField(serialize = false)
    private String id;

    private String value;

    private Date expires;
}
