package org.slf4j;

import com.alibaba.ttl.TransmittableThreadLocal;
import org.slf4j.spi.MDCAdapter;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 自定义 MDC 适配器（继承 TTL 支持异步线程池透传）
 *
 * @author Young
 * @date 2020/12/16
 */
public class AutoLogMDCAdapter implements MDCAdapter {

	private final ThreadLocal<Map<String, String>> copyOnInheritThreadLocal = new TransmittableThreadLocal<>();
	private final ThreadLocal<Map<String, Deque<String>>> dequeThreadLocal = new TransmittableThreadLocal<>();

	private static final int WRITE_OPERATION = 1;
	private static final int MAP_COPY_OPERATION = 2;

	private static AutoLogMDCAdapter autoLogMDCAdapter;

	/**
	 * keeps track of the last operation performed
	 */
	private final ThreadLocal<Integer> lastOperation = new ThreadLocal<>();

	static {
		autoLogMDCAdapter = new AutoLogMDCAdapter();
		installMdcAdapter(autoLogMDCAdapter);
	}

	/**
	 * 获取 Instance 属性值
	 *
	 * @return 处理结果
	 */
	public static MDCAdapter getInstance() {
		return autoLogMDCAdapter;
	}

	private static void installMdcAdapter(MDCAdapter adapter) {
		for (String fieldName : new String[]{"MDC_ADAPTER", "mdcAdapter"}) {
			try {
				Field field = MDC.class.getDeclaredField(fieldName);
				field.setAccessible(true);
				field.set(null, adapter);
				return;
			} catch (ReflectiveOperationException ignored) {
			}
		}
	}

	private Integer getAndSetLastOperation(int op) {
		Integer lastOp = lastOperation.get();
		lastOperation.set(op);
		return lastOp;
	}

	private static boolean wasLastOpReadOrNull(Integer lastOp) {
		return lastOp == null || lastOp == MAP_COPY_OPERATION;
	}

	private Map<String, String> duplicateAndInsertNewMap(Map<String, String> oldMap) {
		Map<String, String> newMap = Collections.synchronizedMap(new HashMap<>());
		if (oldMap != null) {
			synchronized (oldMap) {
				newMap.putAll(oldMap);
			}
		}

		copyOnInheritThreadLocal.set(newMap);
		return newMap;
	}

	/**
	 * put
	 *
	 * @param key 参数 key
	 * @param val 参数 val
	 */
	@Override
	public void put(String key, String val) {
		if (key == null) {
			throw new IllegalArgumentException("key cannot be null");
		}

		Map<String, String> oldMap = copyOnInheritThreadLocal.get();
		Integer lastOp = getAndSetLastOperation(WRITE_OPERATION);

		if (wasLastOpReadOrNull(lastOp) || oldMap == null) {
			Map<String, String> newMap = duplicateAndInsertNewMap(oldMap);
			newMap.put(key, val);
		} else {
			oldMap.put(key, val);
		}
	}

	/**
	 * 移除
	 *
	 * @param key 参数 key
	 */
	@Override
	public void remove(String key) {
		if (key == null) {
			return;
		}
		Map<String, String> oldMap = copyOnInheritThreadLocal.get();
		if (oldMap == null) {
			return;
		}

		Integer lastOp = getAndSetLastOperation(WRITE_OPERATION);

		if (wasLastOpReadOrNull(lastOp)) {
			Map<String, String> newMap = duplicateAndInsertNewMap(oldMap);
			newMap.remove(key);
		} else {
			oldMap.remove(key);
		}
	}

	/**
	 * 清空
	 *
	 */
	@Override
	public void clear() {
		lastOperation.set(WRITE_OPERATION);
		copyOnInheritThreadLocal.remove();
	}

	/**
	 * 获取
	 *
	 * @param key 参数 key
	 * @return 处理结果
	 */
	@Override
	public String get(String key) {
		final Map<String, String> map = copyOnInheritThreadLocal.get();
		if ((map != null) && (key != null)) {
			return map.get(key);
		} else {
			return null;
		}
	}

	/**
	 * 获取 PropertyMap 属性值
	 *
	 * @return 处理结果
	 */
	public Map<String, String> getPropertyMap() {
		lastOperation.set(MAP_COPY_OPERATION);
		return copyOnInheritThreadLocal.get();
	}

	/**
	 * 获取 Keys 属性值
	 *
	 * @return 处理结果
	 */
	public Set<String> getKeys() {
		Map<String, String> map = getPropertyMap();

		if (map != null) {
			return map.keySet();
		} else {
			return null;
		}
	}

	/**
	 * 获取 CopyOfContextMap 属性值
	 *
	 * @return 处理结果
	 */
	@Override
	public Map<String, String> getCopyOfContextMap() {
		Map<String, String> hashMap = copyOnInheritThreadLocal.get();
		if (hashMap == null) {
			return null;
		} else {
			return new HashMap<>(hashMap);
		}
	}

	/**
	 * 设置 ContextMap 属性值
	 *
	 * @param Map<String 参数 Map<String
	 * @param contextMap 参数 contextMap
	 */
	@Override
	public void setContextMap(Map<String, String> contextMap) {
		lastOperation.set(WRITE_OPERATION);

		Map<String, String> newMap = Collections.synchronizedMap(new HashMap<>());
		if (contextMap != null) {
			newMap.putAll(contextMap);
		}

		copyOnInheritThreadLocal.set(newMap);
	}

	/**
	 * push By Key
	 *
	 * @param key 参数 key
	 * @param value 参数 value
	 */
	@Override
	public void pushByKey(String key, String value) {
		if (key == null) {
			return;
		}
		Map<String, Deque<String>> dequeMap = dequeThreadLocal.get();
		if (dequeMap == null) {
			dequeMap = new HashMap<>();
			dequeThreadLocal.set(dequeMap);
		}
		dequeMap.computeIfAbsent(key, ignored -> new ArrayDeque<>()).push(value);
	}

	/**
	 * pop By Key
	 *
	 * @param key 参数 key
	 * @return 处理结果
	 */
	@Override
	public String popByKey(String key) {
		if (key == null) {
			return null;
		}
		Map<String, Deque<String>> dequeMap = dequeThreadLocal.get();
		if (dequeMap == null) {
			return null;
		}
		Deque<String> deque = dequeMap.get(key);
		if (deque == null || deque.isEmpty()) {
			return null;
		}
		return deque.pop();
	}

	/**
	 * 获取 CopyOfDequeByKey 属性值
	 *
	 * @param key 参数 key
	 * @return 处理结果
	 */
	@Override
	public Deque<String> getCopyOfDequeByKey(String key) {
		Map<String, Deque<String>> dequeMap = dequeThreadLocal.get();
		if (dequeMap == null || key == null || dequeMap.get(key) == null) {
			return null;
		}
		return new ArrayDeque<>(dequeMap.get(key));
	}

	/**
	 * 清空 Deque By Key
	 *
	 * @param key 参数 key
	 */
	@Override
	public void clearDequeByKey(String key) {
		Map<String, Deque<String>> dequeMap = dequeThreadLocal.get();
		if (dequeMap != null && key != null) {
			dequeMap.remove(key);
		}
	}
}
