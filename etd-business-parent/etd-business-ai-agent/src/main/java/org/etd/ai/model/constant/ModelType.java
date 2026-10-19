package org.etd.ai.model.constant;

/** 后端当前支持的模型主能力代码，与模型类型字典及DDL保持一致。 */
public final class ModelType {
    /** 文本对话。 */
    public static final String CHAT = "chat";
    /** 文本向量化。 */
    public static final String EMBEDDING = "embedding";
    /** 召回结果重排序。 */
    public static final String RERANK = "rerank";
    /** 图片生成。 */
    public static final String IMAGE = "image";
    /** 语音识别。 */
    public static final String SPEECH_TO_TEXT = "speech_to_text";
    /** 语音合成。 */
    public static final String TEXT_TO_SPEECH = "text_to_speech";
    /** 接口允许维护的类型代码，不表示全部调用能力均已实现。 */
    public static final String PATTERN = CHAT + "|" + EMBEDDING + "|" + RERANK + "|" + IMAGE
            + "|" + SPEECH_TO_TEXT + "|" + TEXT_TO_SPEECH;

    private ModelType() {}
}
