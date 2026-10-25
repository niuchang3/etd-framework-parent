package org.etd.framework.starter.ai.model.constant;

import com.alibaba.cloud.ai.model.RerankModel;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.moderation.ModerationModel;

/** 动态客户端支持的能力契约；具体协议是否实现该能力由适配器声明。 */
public enum ModelCapability {
    /** 聊天及流式聊天。 */
    CHAT("chat", ChatModel.class),
    /** 文本向量化，维度通过单次请求选项传入。 */
    EMBEDDING("embedding", EmbeddingModel.class),
    /** 使用Alibaba统一重排接口。 */
    RERANK("rerank", RerankModel.class),
    /** 图片生成；图片理解继续通过聊天模型的多模态消息调用。 */
    IMAGE("image", ImageModel.class),
    /** 音频文件转文字，与业务模型类型使用同一能力代码。 */
    SPEECH_TO_TEXT("speech_to_text", TranscriptionModel.class),
    /** 文字转语音，支持原生SDK的同步及流式输出。 */
    TEXT_TO_SPEECH("text_to_speech", TextToSpeechModel.class),
    /** 内容审核；输入范围受当前SDK与具体模型支持限制。 */
    MODERATION("moderation", ModerationModel.class);

    /** 与业务配置映射的稳定能力代码。 */
    private final String code;
    /** 客户端必须实现的能力接口。 */
    private final Class<?> modelType;

    ModelCapability(String code, Class<?> modelType) {
        this.code = code;
        this.modelType = modelType;
    }

    /** @return 稳定的能力代码，不使用枚举序号作为协议值 */
    public String getCode() { return code; }
    /** @return 对应Spring AI或Alibaba模型接口 */
    public Class<?> getModelType() { return modelType; }
}
