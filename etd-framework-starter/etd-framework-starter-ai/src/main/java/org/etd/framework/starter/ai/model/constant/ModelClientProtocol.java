package org.etd.framework.starter.ai.model.constant;

/** 内置适配器的协议代码；自定义适配器可使用独立代码，不按厂商名称猜测协议。 */
public final class ModelClientProtocol {
    /** 阿里云原生协议，根地址应包含/api/v1。 */
    public static final String DASHSCOPE = "dashscope";
    /** OpenAI兼容协议，根地址应包含/v1等实际API前缀。 */
    public static final String OPENAI_COMPATIBLE = "openai_compatible";
    /** Ollama原生协议，配置服务根地址。 */
    public static final String OLLAMA = "ollama";
    /** 管理界面允许选择的内置协议，业务校验复用该定义，不代表所有协议都支持全部能力。 */
    public static final String PATTERN = DASHSCOPE + "|" + OLLAMA + "|" + OPENAI_COMPATIBLE;
    private ModelClientProtocol() { }
}
