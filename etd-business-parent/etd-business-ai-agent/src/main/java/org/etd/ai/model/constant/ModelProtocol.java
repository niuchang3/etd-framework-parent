package org.etd.ai.model.constant;

/** 模型配置协议代码与页面校验范围，必须与初始化字典及DDL保持一致。 */
public final class ModelProtocol {
    /** 不使用认证凭证。 */
    public static final String AUTH_NONE = "none";

    /** 使用只写的API Key凭证。 */
    public static final String AUTH_API_KEY = "api_key";

    /** 当前允许保存的接入协议代码，不表示已实现实际调用。 */
    public static final String PROVIDER_PATTERN = "dashscope|ollama|openai_compatible";

    /** 当前支持保存的认证类型。 */
    public static final String AUTH_PATTERN = "none|api_key";

    /** 当前支持保存的模型主能力全集。 */
    public static final String TYPE_PATTERN =
            "chat|embedding|rerank|image|speech_to_text|text_to_speech";

    private ModelProtocol() {}
}
