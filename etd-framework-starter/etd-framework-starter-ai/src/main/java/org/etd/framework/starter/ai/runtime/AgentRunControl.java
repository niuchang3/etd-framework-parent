package org.etd.framework.starter.ai.runtime;

import java.util.List;
import reactor.core.Disposable;
import java.time.Duration;
import com.alibaba.cloud.ai.graph.agent.tool.CancellationToken;
import org.springframework.ai.document.Document;

/** 框架内部 Run 控制与进度协作；业务入口仅返回 Reactor Disposable，不暴露线程和用量维护方法。 */
public interface AgentRunControl extends Disposable {
    /** @return 当前执行标识 */
    String runId();

    /** 记录实际纳入上下文的引用，空列表也必须产生一次 REFERENCES 事件。 */
    void recordReferences(List<Document> references);

    /** 模型每轮的累计用量水位线，由执行适配捕获原始 Usage，禁止逐分片相加。 */
    void recordUsage(Long promptTokens, Long completionTokens);

    /** 新模型调用开始；将上一轮水位线归入 Run 累计值。 */
    void nextCallRound();

    /** 执行前与每次外部调用前校验取消、超时或致命授权失败。 */
    void requireActive();

    /** 登记正在执行的工具线程；与取消原子协调，防止登记后漏掉中断。 */
    void registerToolThread();

    /** 工具调用退出时移除线程登记，避免中断已复用的线程。 */
    void unregisterToolThread();

    /** @return 官方协作式取消令牌，与 Run 超时、断开及主动停止共同绑定 */
    CancellationToken cancellationToken();

    /** @return Run 剩余期限，工具自身超时不能长于此期限 */
    Duration remainingTime();

    /** 授权或基础设施失败必须终止整轮，不能被官方工具错误转为可重试观察。 */
    void fail();
}
