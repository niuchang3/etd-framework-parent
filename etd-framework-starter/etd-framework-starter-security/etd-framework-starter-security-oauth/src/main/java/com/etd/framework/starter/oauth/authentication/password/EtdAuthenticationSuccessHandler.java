package com.etd.framework.starter.oauth.authentication.password;

import com.etd.framework.starter.client.core.encrypt.TokenEncoder;
import org.etd.framework.common.core.model.ResultModel;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class EtdAuthenticationSuccessHandler extends AbstractAuthenticationHandler implements AuthenticationSuccessHandler {


    private TokenEncoder<Authentication> tokenEncoder;

    /**
     * Token编码器
     *
     * @param tokenEncoder
     */
    public EtdAuthenticationSuccessHandler(TokenEncoder tokenEncoder) {
        this.tokenEncoder = tokenEncoder;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        ServletServerHttpResponse httpResponse = new ServletServerHttpResponse(response);
        httpResponse.setStatusCode(org.springframework.http.HttpStatus.OK);
        String token = tokenEncoder.encoder(authentication);
        ResultModel<String> success = ResultModel.success(token);
        getConverter().write(success, MediaType.APPLICATION_JSON, httpResponse);

    }
}
