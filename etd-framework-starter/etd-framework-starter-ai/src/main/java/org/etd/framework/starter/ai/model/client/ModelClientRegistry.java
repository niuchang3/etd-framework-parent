package org.etd.framework.starter.ai.model.client;

import com.alibaba.cloud.ai.model.RerankModel;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.LongSupplier;
import org.etd.framework.starter.ai.model.config.ModelClientProperties;
import org.etd.framework.starter.ai.model.config.ModelConnectionConfiguration;
import org.etd.framework.starter.ai.model.constant.ModelCapability;
import org.etd.framework.starter.ai.model.spi.ModelConnectionResolver;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.moderation.ModerationModel;
import org.springframework.ai.model.Model;
import org.springframework.util.Assert;
import java.util.Iterator;

/**
 * 模型访问入口：每次先授权，再通过有界本地Map复用原生SDK实例，不依赖JetCache或Caffeine。
 * 缓存按租户、连接、配置版本和能力区分；过期及LRU淘汰只移除引用，不关闭在途请求使用的模型。
 */
public class ModelClientRegistry implements AutoCloseable {
    private final ModelClientFactory factory;
    private final ModelConnectionResolver resolver;
    private final int maximumSize;
    private final long expireNanos;
    private final LongSupplier ticker;
    /** 按访问顺序排列；所有Map访问均在本对象监视器内，SDK创建在锁外执行。 */
    private final LinkedHashMap<ModelClientKey, CachedModel> cache = new LinkedHashMap<>(16, 0.75f, true);
    private boolean closed;

    /** @param factory SDK实例工厂 @param resolver 授权解析器 @param properties 缓存容量、过期及HTTP配置 */
    public ModelClientRegistry(ModelClientFactory factory, ModelConnectionResolver resolver, ModelClientProperties properties) {
        this(factory, resolver, properties, System::nanoTime);
    }

    /** 使用可替换单调时钟验证过期行为，生产不创建后台扫描线程。 */
    ModelClientRegistry(ModelClientFactory factory, ModelConnectionResolver resolver,
            ModelClientProperties properties, LongSupplier ticker) {
        properties.validate();
        this.factory = factory;
        this.resolver = resolver;
        this.maximumSize = properties.getMaximumSize();
        this.expireNanos = properties.getExpireAfterAccess().toNanos();
        this.ticker = ticker;
    }

    /** @param tenantId 可信连接归属租户 @param connectionId 连接 @return 原生聊天模型，不应跨业务请求自行长期缓存 */
    public ChatModel getChatModel(String tenantId, String connectionId) {
        return getModel(tenantId, connectionId, ModelCapability.CHAT, ChatModel.class);
    }

    /** @param tenantId 可信连接归属租户 @param connectionId 连接 @return 原生向量模型，维度等选项由单次请求提供 */
    public EmbeddingModel getEmbeddingModel(String tenantId, String connectionId) {
        return getModel(tenantId, connectionId, ModelCapability.EMBEDDING, EmbeddingModel.class);
    }

    /** @param tenantId 可信连接归属租户 @param connectionId 连接 @return Alibaba原生重排接口实现 */
    public RerankModel getRerankModel(String tenantId, String connectionId) {
        return getModel(tenantId, connectionId, ModelCapability.RERANK, RerankModel.class);
    }

    /** @param tenantId 可信连接归属租户 @param connectionId 图片生成连接 @return 原生图片模型，尺寸等选项由单次请求提供 */
    public ImageModel getImageModel(String tenantId, String connectionId) {
        return getModel(tenantId, connectionId, ModelCapability.IMAGE, ImageModel.class);
    }

    /** @param tenantId 可信连接归属租户 @param connectionId 语音识别连接 @return 原生转录模型，调用方负责音频资源授权 */
    public TranscriptionModel getTranscriptionModel(String tenantId, String connectionId) {
        return getModel(tenantId, connectionId, ModelCapability.SPEECH_TO_TEXT, TranscriptionModel.class);
    }

    /** @param tenantId 可信连接归属租户 @param connectionId 语音合成连接 @return 原生语音模型，音色等选项由单次请求提供 */
    public TextToSpeechModel getTextToSpeechModel(String tenantId, String connectionId) {
        return getModel(tenantId, connectionId, ModelCapability.TEXT_TO_SPEECH, TextToSpeechModel.class);
    }

    /** @param tenantId 可信连接归属租户 @param connectionId 内容审核连接 @return 原生审核模型，业务自行解释审核结果 */
    public ModerationModel getModerationModel(String tenantId, String connectionId) {
        return getModel(tenantId, connectionId, ModelCapability.MODERATION, ModerationModel.class);
    }

    /** 每次先授权，再确定是否由当前请求创建实例，最后取得共享创建结果。 */
    private <T> T getModel(String tenantId, String connectionId, ModelCapability capability, Class<T> type) {
        requireOpen();
        ModelConnectionConfiguration configuration = resolveAuthorizedConfiguration(tenantId, connectionId, capability);
        ModelClientKey key = new ModelClientKey(tenantId, connectionId, configuration.version(), capability);
        ModelCacheLookup lookup = findOrCreateCacheEntry(key);
        if (lookup.initializationRequired()) {
            // 只有首次登记缓存的请求负责创建SDK；创建在缓存锁外执行。
            initializeCachedModel(key, lookup.cachedModel(), configuration);
        }
        Model<?, ?> model = awaitCachedModel(lookup.cachedModel());
        return type.cast(model);
    }

    /** 已完成的实例直接返回；其他请求正在创建时等待同一个Future，不重复创建SDK。 */
    private Model<?, ?> awaitCachedModel(CachedModel cached) {
        try {
            return cached.modelFuture.join();
        } catch (CompletionException exception) {
            if (exception.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw exception;
        }
    }

    /** 授权不缓存；较早开始的请求可完成旧版本，新请求依据最新配置版本选实例。 */
    private ModelConnectionConfiguration resolveAuthorizedConfiguration(String tenantId, String connectionId,
            ModelCapability capability) {
        Assert.hasText(tenantId, "租户标识不能为空");
        Assert.hasText(connectionId, "连接标识不能为空");
        ModelConnectionConfiguration configuration = resolver.resolveAuthorized(tenantId, connectionId);
        Assert.notNull(configuration, "未取得已授权的模型连接配置");
        Assert.isTrue(tenantId.equals(configuration.tenantId()) && connectionId.equals(configuration.connectionId()),
                "模型连接归属与请求不匹配");
        Assert.state(configuration.enabled(), "模型连接已停用");
        Assert.isTrue(configuration.capability() == capability, "模型连接能力不匹配");
        return configuration;
    }

    /** 锁内查找已有实例或登记创建位置；并发请求取得相同缓存项，并刷新最近访问时间。 */
    private synchronized ModelCacheLookup findOrCreateCacheEntry(ModelClientKey key) {
        requireOpen();
        long now = ticker.getAsLong();
        removeExpired(now);
        CachedModel existing = cache.get(key);
        if (existing != null) {
            existing.lastAccessNanos = now;
            return new ModelCacheLookup(existing, false);
        }
        CachedModel created = new CachedModel(now);
        cache.put(key, created);
        removeLeastRecentlyUsedModels();
        return new ModelCacheLookup(created, true);
    }

    /** 调用方持有缓存锁；LinkedHashMap按访问顺序排列，容量超限时移除最久未使用的引用。 */
    private void removeLeastRecentlyUsedModels() {
        while (cache.size() > maximumSize) {
            Iterator<ModelClientKey> iterator = cache.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
    }

    /** 失效期间已移除的槽位不会被初始化完成回调重新放回Map。 */
    private void initializeCachedModel(ModelClientKey key, CachedModel cached, ModelConnectionConfiguration configuration) {
        try {
            Model<?, ?> model = factory.create(configuration);
            cached.modelFuture.complete(model);
        } catch (RuntimeException | Error exception) {
            cached.modelFuture.completeExceptionally(exception);
            synchronized (this) {
                cache.remove(key, cached);
            }
        }
    }

    /** @param tenantId 连接所属租户 @param connectionId 连接；移除全部已缓存版本，不关闭原生模型 */
    public synchronized void invalidate(String tenantId, String connectionId) {
        Assert.hasText(tenantId, "租户标识不能为空");
        Assert.hasText(connectionId, "连接标识不能为空");
        cache.keySet().removeIf(key -> tenantId.equals(key.tenantId()) && connectionId.equals(key.connectionId()));
    }

    /** 显式清理过期引用；普通获取时也执行，不依赖定时扫描线程。 */
    public synchronized void cleanUp() { removeExpired(ticker.getAsLong()); }

    private void removeExpired(long now) {
        cache.values().removeIf(cached -> now - cached.lastAccessNanos >= expireNanos);
    }

    private synchronized void requireOpen() {
        Assert.state(!closed, "模型客户端管理器已关闭");
    }

    /** 应用关闭时清空缓存引用并拒绝新获取；共享HTTP传输独立关闭。 */
    @Override
    public synchronized void close() {
        closed = true;
        cache.clear();
    }

    /** 只包含身份及版本，不保存地址、凭证或会话状态。 */
    private record ModelClientKey(String tenantId, String connectionId, long version, ModelCapability capability) { }
    /** 缓存查找结果，首次登记者创建实例，其余请求等待共享结果。
     * @param cachedModel 当前缓存项 @param initializationRequired 是否由本次请求初始化SDK
     */
    private record ModelCacheLookup(CachedModel cachedModel, boolean initializationRequired) { }
    /** 缓存槽位只记录共享Future和最近访问时间，不维护租约计数或独占资源。 */
    private static final class CachedModel {
        /** 共享实例创建结果，避免同一缓存键并发重复创建；失败时也通知所有等待者。 */
        private final CompletableFuture<Model<?, ?>> modelFuture = new CompletableFuture<>();
        /** 最近访问的单调时钟纳秒值，用于空闲过期判断。 */
        private long lastAccessNanos;

        private CachedModel(long lastAccessNanos) {
            this.lastAccessNanos = lastAccessNanos;
        }
    }
}
