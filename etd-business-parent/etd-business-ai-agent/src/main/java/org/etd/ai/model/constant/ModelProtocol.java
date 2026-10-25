package org.etd.ai.model.constant;

/** 连接器认证代码；接入协议使用AI starter的ModelClientProtocol统一定义。 */
public final class ModelProtocol {
    /** 不使用认证凭证。 */
    public static final String AUTH_NONE = "none";

    /** 使用只写的API Key凭证。 */
    public static final String AUTH_API_KEY = "api_key";

    /** 当前支持保存的认证类型。 */
    public static final String AUTH_PATTERN = "none|api_key";

    private ModelProtocol() {}
}
