package org.etd.ai.model.constant;

/** 连接器协议及认证代码，必须与初始化字典及DDL保持一致；不代表已验证调用能力。 */
public final class ModelProtocol {
    /** 不使用认证凭证。 */
    public static final String AUTH_NONE = "none";

    /** 使用只写的API Key凭证。 */
    public static final String AUTH_API_KEY = "api_key";

    /** 阿里云原生DashScope协议。 */
    public static final String DASHSCOPE = "dashscope";
    /** Ollama原生协议。 */
    public static final String OLLAMA = "ollama";
    /** OpenAI兼容协议。 */
    public static final String OPENAI_COMPATIBLE = "openai_compatible";

    /** 当前允许保存的接入协议代码，不表示已实现全部模型类型调用。 */
    public static final String PROTOCOL_PATTERN = DASHSCOPE + "|" + OLLAMA + "|" + OPENAI_COMPATIBLE;

    /** 当前支持保存的认证类型。 */
    public static final String AUTH_PATTERN = "none|api_key";

    private ModelProtocol() {}
}
