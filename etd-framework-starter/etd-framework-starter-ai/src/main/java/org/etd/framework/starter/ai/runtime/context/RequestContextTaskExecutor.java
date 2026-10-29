package org.etd.framework.starter.ai.runtime.context;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/** 带请求上下文的任务执行入口，统一处理聊天、定时维护和多库并发中的上下文传递。 */
public final class RequestContextTaskExecutor {
    private RequestContextTaskExecutor() {
    }

    /** @param executor 调用方管理生命周期的线程池 @param action 业务任务
     * @return 已提交任务；执行时恢复提交方身份，退出后恢复工作线程原上下文
     */
    public static Future<?> submit(ExecutorService executor, Runnable action) {
        Runnable contextualAction = wrapAction(action);
        return executor.submit(contextualAction);
    }

    /** 在注册时捕获上下文，每次执行都独立恢复，定时线程不能依赖原HTTP请求仍然存活。
     * @param executor 调用方管理的定时线程池 @param action 定时任务
     * @param initialDelay 首次等待时间 @param delay 两次执行之间的等待时间 @param unit 时间单位
     * @return 可取消的定时任务
     */
    public static ScheduledFuture<?> scheduleWithFixedDelay(ScheduledExecutorService executor, Runnable action,
            long initialDelay, long delay, TimeUnit unit) {
        Runnable contextualAction = wrapAction(action);
        return executor.scheduleWithFixedDelay(contextualAction, initialDelay, delay, unit);
    }

    /** 并发处理一批输入并按原顺序返回，失败或中断时取消剩余任务，不复用不同请求的上下文。
     * @param inputs 本批输入 @param action 单项处理逻辑 @param <I> 输入类型 @param <O> 结果类型
     * @return 与输入顺序一致的结果列表
     * @throws RuntimeException 子任务失败原样传播；等待被中断时保留中断标记
     */
    public static <I, O> List<O> invokeList(List<I> inputs, Function<I, O> action) {
        RequestContextSnapshot context = RequestContextSnapshot.capture();
        List<Future<O>> tasks = new ArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            try {
                for (I input : inputs) {
                    Supplier<O> task = () -> action.apply(input);
                    Callable<O> contextualTask = () -> context.call(task);
                    Future<O> future = executor.submit(contextualTask);
                    tasks.add(future);
                }
                return awaitResultList(tasks);
            } finally {
                cancelTaskList(tasks);
            }
        }
    }

    /** 捕获发生在提交方；回调内只执行任务，不再次读取提交线程已经变化的上下文。 */
    private static Runnable wrapAction(Runnable action) {
        RequestContextSnapshot context = RequestContextSnapshot.capture();
        return () -> context.run(action);
    }

    /** 等待保留原始顺序和业务异常，不能将线程中断吞掉后继续执行聊天。 */
    private static <O> List<O> awaitResultList(List<Future<O>> tasks) {
        List<O> results = new ArrayList<>();
        try {
            for (Future<O> task : tasks) {
                O result = task.get();
                results.add(result);
            }
            return results;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("并发任务等待已中断", exception);
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("并发任务执行异常", exception.getCause());
        }
    }

    /** 先停止未完成任务，再关闭执行器，避免异常后继续等待无用的数据库或模型调用。 */
    private static <O> void cancelTaskList(List<Future<O>> tasks) {
        for (Future<O> task : tasks) {
            task.cancel(true);
        }
    }
}
