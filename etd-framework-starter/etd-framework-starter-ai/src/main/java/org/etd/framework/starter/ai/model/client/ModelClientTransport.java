package org.etd.framework.starter.ai.model.client;

import java.net.http.HttpClient;
import java.util.Map;
import org.springframework.web.reactive.function.client.ClientRequest;
import reactor.core.publisher.Mono;
import org.springframework.retry.support.RetryTemplate;
import org.etd.framework.starter.ai.model.config.ModelClientProperties;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/** 共享JDK HTTP连接池；每次返回独立Builder，认证只保存在对应连接的SDK客户端中。 */
public class ModelClientTransport implements AutoCloseable {
    /** 禁止框架透明重试模型请求，避免重复计费和重复工具执行。 */
    private static final int SDK_MAX_ATTEMPTS = 1;
    private final HttpClient client;
    private final JdkClientHttpRequestFactory requestFactory;
    private final JdkClientHttpConnector connector;

    /** @param properties 连接及响应超时；不在构造时访问任何模型服务 */
    public ModelClientTransport(ModelClientProperties properties) {
        properties.validate();
        this.client = HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout()).build();
        this.requestFactory = new JdkClientHttpRequestFactory(client);
        requestFactory.setReadTimeout(properties.getReadTimeout());
        this.connector = new JdkClientHttpConnector(client);
        connector.setReadTimeout(properties.getReadTimeout());
    }

    /** @return 单连接专用Builder，禁止跨租户共享其可变默认请求头 */
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder().requestFactory(requestFactory).requestInterceptor((request, body, execution) -> {
            // 同步Embedding/Rerank在调用线程读取上下文，不能在客户端创建时捕获。
            ModelRequestHeaders.capture().forEach((key, value) -> request.getHeaders().set(key, value));
            return execution.execute(request, body);
        });
    }

    /** @return 单连接专用流式Builder，与同步调用共用HTTP连接池 */
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder().clientConnector(connector).filter((request, next) -> Mono.deferContextual(context -> {
            Map<String, String> headers = context.getOrDefault(ModelRequestHeaders.REACTOR_KEY, Map.of());
            ClientRequest.Builder builder = ClientRequest.from(request);
            builder.headers(outgoing -> headers.forEach(outgoing::set));
            ClientRequest outgoing = builder.build();
            return next.exchange(outgoing);
        }));
    }

    /** @return 明确只尝试一次的SDK调用策略，由上层决定是否重试完整业务动作 */
    public RetryTemplate retryTemplate() {
        return RetryTemplate.builder().maxAttempts(SDK_MAX_ATTEMPTS).build();
    }

    /** 应用关闭时等待在途HTTP调用结束，然后释放共享连接池。 */
    @Override
    public void close() { client.close(); }
}
