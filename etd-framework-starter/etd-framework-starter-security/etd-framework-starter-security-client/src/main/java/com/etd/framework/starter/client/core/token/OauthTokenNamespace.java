package com.etd.framework.starter.client.core.token;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.N;

import java.io.Serializable;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class OauthTokenNamespace implements Serializable {


    private static final long serialVersionUID = 5875711995884719731L;


    private String accessId;

    private String refreshId;

}
