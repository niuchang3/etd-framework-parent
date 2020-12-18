package org.etd.framework.starter.log.thread;

import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * @author Young
 * @description
 * @date 2020/12/16
 */
public class CustomizeThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {


	/**
	 * 把父线程的MDC内容赋值给子线程
	 *
	 * @param runnable
	 */
	@Override
	public void execute(Runnable runnable) {
		Map<String, String> mdcContext = MDC.getCopyOfContextMap();
		super.execute(() -> run(runnable, mdcContext));
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		Map<String, String> mdcContext = MDC.getCopyOfContextMap();
		return super.submit(() -> call(task, mdcContext));
	}

	/**
	 * 子线程委托的执行方法
	 *
	 * @param runnable   {@link Runnable}
	 * @param mdcContext 父线程MDC内容
	 */
	private void run(Runnable runnable, Map<String, String> mdcContext) {
		// 将父线程的MDC内容传给子线程
		if (mdcContext != null) {
			MDC.setContextMap(mdcContext);
		}
		try {
			// 执行异步操作
			runnable.run();
		} finally {
			// 清空MDC内容
			MDC.clear();
		}
	}

	/**
	 * 子线程委托的执行方法
	 *
	 * @param task       {@link Callable}
	 * @param mdcContext 父线程MDC内容
	 */
	private <T> T call(Callable<T> task, Map<String, String> mdcContext) throws Exception {
		// 将父线程的MDC内容传给子线程
		if (mdcContext != null) {
			MDC.setContextMap(mdcContext);
		}
		try {
			// 执行异步操作
			return task.call();
		} finally {
			// 清空MDC内容
			MDC.clear();
		}
	}
}
