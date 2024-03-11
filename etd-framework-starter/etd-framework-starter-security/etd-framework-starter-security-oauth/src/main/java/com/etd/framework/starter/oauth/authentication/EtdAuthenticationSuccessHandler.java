package com.etd.framework.starter.oauth.authentication;

import com.etd.framework.starter.client.core.constant.Oauth2ParameterConstant;
import com.etd.framework.starter.client.core.encrypt.TokenEncoder;
import com.etd.framework.starter.client.core.properties.SystemOauthProperties;
import com.etd.framework.starter.client.core.token.OauthToken;
import com.etd.framework.starter.client.core.token.OauthTokenValue;
import org.etd.framework.common.core.model.ResultModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class EtdAuthenticationSuccessHandler extends AbstractAuthenticationHandler implements AuthenticationSuccessHandler {


    private TokenEncoder<Authentication, OauthTokenValue> tokenEncoder;

    private SystemOauthProperties oauthProperties;

    /**
     * Token编码器
     *
     * @param tokenEncoder
     */
    public EtdAuthenticationSuccessHandler(TokenEncoder tokenEncoder, SystemOauthProperties oauthProperties) {
        this.tokenEncoder = tokenEncoder;
        this.oauthProperties = oauthProperties;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        OauthTokenValue accessToken = tokenEncoder.encode(Oauth2ParameterConstant.TokenType.access_token, authentication);
        OauthTokenValue refreshToken = null;
        if (oauthProperties.getAccessToken().getEnabled()) {
            refreshToken = tokenEncoder.encode(Oauth2ParameterConstant.TokenType.refresh_token, authentication);
        }

        OauthToken token = new OauthToken();
        token.setTokenType(Oauth2ParameterConstant.TokenPrompt.Bearer.name());
        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);
        ResultModel<OauthToken> success = ResultModel.success(token);
        writeSuccess(response, success);

    }
}
