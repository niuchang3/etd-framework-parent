package org.etd.framework.starter.ai.model.client;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.etd.framework.starter.ai.model.config.ModelClientProperties;
import org.etd.framework.starter.ai.model.config.ModelConnectionConfiguration;
import org.etd.framework.starter.ai.model.constant.ModelCapability;
import org.etd.framework.starter.ai.model.constant.ModelClientProtocol;
import org.etd.framework.starter.ai.model.spi.ModelClientProvider;
import org.etd.framework.starter.ai.model.spi.ModelConnectionResolver;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.Model;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;

/** 验证有界Map的授权、并发、版本和过期行为；返回原生模型，不再使用Instance或Lease。 */
class ModelClientRegistryTest {
    /** 同一缓存键并发只初始化一次，但每次获取仍执行授权。 */
    @Test
    void shouldInitializeOnceForConcurrentGets() throws Exception {
        AtomicInteger created = new AtomicInteger();
        AtomicInteger resolved = new AtomicInteger();
        ModelClientFactory factory = factory(created);
        ModelConnectionConfiguration configuration = configuration("a", "1", 1, true);
        ModelConnectionResolver resolver = (tenant, id) -> { resolved.incrementAndGet(); return configuration; };
        ModelClientProperties properties = new ModelClientProperties();
        try (ModelClientRegistry registry = new ModelClientRegistry(factory, resolver, properties);
             ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch start = new CountDownLatch(1);
            ArrayList<Future<ChatModel>> tasks = new ArrayList<Future<ChatModel>>();
            for (int index = 0; index < 32; index++) {
                Future<ChatModel> task = executor.submit(() -> { await(start); return registry.getChatModel("a", "1"); });
                tasks.add(task);
            }
            start.countDown();
            ChatModel first = tasks.getFirst().get(5, TimeUnit.SECONDS);
            for (Future<ChatModel> task : tasks) { assertThat(task.get(5, TimeUnit.SECONDS)).isSameAs(first); }
            assertThat(created).hasValue(1);
            assertThat(resolved).hasValue(32);
        }
    }

    /** 不同租户、不同配置版本分别缓存；获取者仍持有的旧模型不会被关闭。 */
    @Test
    void shouldSeparateTenantsAndConfigurationVersions() {
        AtomicInteger created = new AtomicInteger();
        AtomicLong revision = new AtomicLong(1);
        ModelClientFactory factory = factory(created);
        ModelConnectionResolver resolver = (tenant, id) -> configuration(tenant, id, revision.get(), true);
        ModelClientProperties properties = new ModelClientProperties();
        try (ModelClientRegistry registry = new ModelClientRegistry(factory, resolver, properties)) {
            ChatModel first = registry.getChatModel("a", "1");
            ChatModel other = registry.getChatModel("b", "1");
            assertThat(first).isNotSameAs(other);
            revision.set(2);
            ChatModel updated = registry.getChatModel("a", "1");
            assertThat(updated).isNotSameAs(first);
            assertThat(first.call("仍在执行的请求")).isEqualTo("ok");
            assertThat(registry.getChatModel("a", "1")).isSameAs(updated);
            assertThat(created).hasValue(3);
        }
    }

    /** 停用或授权解析失败时，不能因为缓存命中继续放行。 */
    @Test
    void shouldAuthorizeEveryGetAndRejectDisabledOrWrongTenant() {
        ModelConnectionConfiguration initial = configuration("a", "1", 1, true);
        assertThat(initial.toString()).doesNotContain("example.invalid", "test-key", "test-model");
        AtomicReference<ModelConnectionConfiguration> current = new AtomicReference<>(initial);
        ModelClientFactory factory = factory(new AtomicInteger());
        ModelClientProperties properties = new ModelClientProperties();
        ModelConnectionResolver resolver = (tenant, id) -> current.get();
        try (ModelClientRegistry registry = new ModelClientRegistry(factory, resolver, properties)) {
            ChatModel old = registry.getChatModel("a", "1");
            ModelConnectionConfiguration disabled = configuration("a", "1", 2, false);
            current.set(disabled);
            assertThatThrownBy(() -> registry.getChatModel("a", "1")).hasMessageContaining("停用");
            assertThat(old.call("在途请求")).isEqualTo("ok");
            ModelConnectionConfiguration wrongTenant = configuration("b", "1", 3, true);
            current.set(wrongTenant);
            assertThatThrownBy(() -> registry.getChatModel("a", "1")).hasMessageContaining("归属");
            current.set(initial);
            assertThatThrownBy(() -> registry.getEmbeddingModel("a", "1")).hasMessageContaining("能力");
        }
    }

    /** 验证LRU容量上限、访问续期及空闲过期；使用单调测试时钟避免sleep。 */
    @Test
    void shouldEvictLruAndExpireIdleModelsWithBoundedMap() {
        AtomicInteger created = new AtomicInteger();
        AtomicLong clock = new AtomicLong();
        ModelClientProperties properties = new ModelClientProperties();
        properties.setMaximumSize(2);
        properties.setExpireAfterAccess(Duration.ofNanos(10));
        ModelConnectionResolver resolver = (tenant, id) -> configuration(tenant, id, 1, true);
        ModelClientFactory factory = factory(created);
        try (ModelClientRegistry registry = new ModelClientRegistry(factory, resolver, properties, clock::get)) {
            ChatModel first = registry.getChatModel("a", "1");
            ChatModel second = registry.getChatModel("a", "2");
            assertThat(registry.getChatModel("a", "1")).isSameAs(first);
            registry.getChatModel("a", "3");
            assertThat(registry.getChatModel("a", "2")).isNotSameAs(second);
            assertThat(second.call("淘汰后的在途请求")).isEqualTo("ok");
            clock.set(6);
            ChatModel active = registry.getChatModel("a", "2");
            clock.set(11);
            assertThat(registry.getChatModel("a", "2")).isSameAs(active);
            clock.set(22);
            registry.cleanUp();
            assertThat(registry.getChatModel("a", "2")).isNotSameAs(active);
            assertThat(created).hasValue(5);
        }
    }

    /** 初始化失败移除槽位，下次可重试；原始SDK异常不作为cause向外泄露。 */
    @Test
    void shouldRetryFailedInitializationWithoutLeakingSecrets() {
        AtomicInteger created = new AtomicInteger();
        Function<ModelConnectionConfiguration, Model<?, ?>> creator = configuration -> {
            if (created.incrementAndGet() == 1) { throw new IllegalArgumentException("sensitive-credential"); }
            return chatModel();
        };
        ModelClientFactory factory = factory(creator);
        ModelClientProperties properties = new ModelClientProperties();
        ModelConnectionResolver resolver = (tenant, id) -> configuration(tenant, id, 1, true);
        try (ModelClientRegistry registry = new ModelClientRegistry(factory, resolver, properties)) {
            assertThatThrownBy(() -> registry.getChatModel("a", "1"))
                    .hasMessageNotContaining("sensitive-credential").hasNoCause();
            assertThat(registry.getChatModel("a", "1")).isNotNull();
            assertThat(created).hasValue(2);
        }
    }

    /** 主动失效清理全部版本；关闭仅清空引用，不破坏已经交给调用方的原生模型。 */
    @Test
    void shouldInvalidateAllVersionsAndRejectGetsAfterShutdown() {
        AtomicLong version = new AtomicLong(1);
        ModelClientFactory factory = factory(new AtomicInteger());
        ModelClientProperties properties = new ModelClientProperties();
        ModelConnectionResolver resolver = (tenant, id) -> configuration(tenant, id, version.get(), true);
        ModelClientRegistry registry = new ModelClientRegistry(factory, resolver, properties);
        try {
            ChatModel first = registry.getChatModel("a", "1");
            version.set(2);
            ChatModel second = registry.getChatModel("a", "1");
            registry.invalidate("a", "1");
            assertThat(registry.getChatModel("a", "1")).isNotSameAs(second);
            version.set(1);
            assertThat(registry.getChatModel("a", "1")).isNotSameAs(first);
            registry.close();
            assertThat(first.call("已发起请求")).isEqualTo("ok");
            assertThatThrownBy(() -> registry.getChatModel("a", "1")).hasMessageContaining("关闭");
        } finally {
            registry.close();
        }
    }

    /** 初始化期间失效后，旧初始化结果不会重新写入缓存覆盖新实例。 */
    @Test
    void shouldNotReinsertInvalidatedInitialization() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);
        AtomicInteger created = new AtomicInteger();
        Function<ModelConnectionConfiguration, Model<?, ?>> creator = configuration -> {
            if (created.incrementAndGet() == 1) { entered.countDown(); await(proceed); }
            return chatModel();
        };
        ModelClientFactory factory = factory(creator);
        ModelClientProperties properties = new ModelClientProperties();
        ModelConnectionResolver resolver = (tenant, id) -> configuration(tenant, id, 1, true);
        try (ModelClientRegistry registry = new ModelClientRegistry(factory, resolver, properties);
             ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<ChatModel> initial = executor.submit(() -> registry.getChatModel("a", "1"));
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            try {
                registry.invalidate("a", "1");
                ChatModel current = registry.getChatModel("a", "1");
                proceed.countDown();
                assertThat(initial.get(5, TimeUnit.SECONDS)).isNotSameAs(current);
                assertThat(registry.getChatModel("a", "1")).isSameAs(current);
            } finally {
                proceed.countDown();
            }
        }
    }

    /** 旧配置请求较晚返回，只生成旧版本键，不会覆盖后续请求正在使用的新版本键。 */
    @Test
    void shouldKeepLatestVersionWhenAnOlderRequestFinishesLater() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);
        AtomicInteger resolutions = new AtomicInteger();
        ModelConnectionResolver resolver = (tenant, id) -> {
            if (resolutions.incrementAndGet() == 1) {
                entered.countDown();
                await(proceed);
                return configuration(tenant, id, 1, true);
            }
            return configuration(tenant, id, 2, true);
        };
        ModelClientFactory factory = factory(new AtomicInteger());
        ModelClientProperties properties = new ModelClientProperties();
        try (ModelClientRegistry registry = new ModelClientRegistry(factory, resolver, properties);
             ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<ChatModel> initial = executor.submit(() -> registry.getChatModel("a", "1"));
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            try {
                ChatModel current = registry.getChatModel("a", "1");
                proceed.countDown();
                assertThat(initial.get(5, TimeUnit.SECONDS)).isNotSameAs(current);
                assertThat(registry.getChatModel("a", "1")).isSameAs(current);
            } finally {
                proceed.countDown();
            }
        }
    }

    /** 并发测试使用有界栅栏等待，不根据sleep猜测执行顺序。 */
    private void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("测试等待被中断", exception);
        }
    }

    private ModelClientFactory factory(AtomicInteger created) {
        Function<ModelConnectionConfiguration, Model<?, ?>> creator = configuration -> {
            created.incrementAndGet();
            return chatModel();
        };
        return factory(creator);
    }

    private ModelClientFactory factory(Function<ModelConnectionConfiguration, Model<?, ?>> creator) {
        ModelClientProvider provider = new ModelClientProvider() {
            @Override
            public boolean supports(String protocol, ModelCapability capability) { return true; }
            @Override
            public Model<?, ?> create(ModelConnectionConfiguration configuration) { return creator.apply(configuration); }
        };
        List<ModelClientProvider> providers = List.of(provider);
        return new ModelClientFactory(providers);
    }

    private ChatModel chatModel() {
        ChatModel model = mock(ChatModel.class);
        when(model.call(anyString())).thenReturn("ok");
        return model;
    }

    private ModelConnectionConfiguration configuration(String tenant, String id, long version, boolean enabled) {
        URI uri = URI.create("https://example.invalid/v1");
        return new ModelConnectionConfiguration(tenant, id, version, enabled, ModelClientProtocol.OPENAI_COMPATIBLE,
                ModelCapability.CHAT, "test-model", uri, () -> "test-key");
    }
}
