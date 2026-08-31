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

	@Override
	public void clear() {
		lastOperation.set(WRITE_OPERATION);
		copyOnInheritThreadLocal.remove();
	}

	@Override
	public String get(String key) {
		final Map<String, String> map = copyOnInheritThreadLocal.get();
		if ((map != null) && (key != null)) {
			return map.get(key);
		} else {
			return null;
		}
	}

	public Map<String, String> getPropertyMap() {
		lastOperation.set(MAP_COPY_OPERATION);
		return copyOnInheritThreadLocal.get();
	}

	public Set<String> getKeys() {
		Map<String, String> map = getPropertyMap();

		if (map != null) {
			return map.keySet();
		} else {
			return null;
		}
	}

	@Override
	public Map<String, String> getCopyOfContextMap() {
		Map<String, String> hashMap = copyOnInheritThreadLocal.get();
		if (hashMap == null) {
			return null;
		} else {
			return new HashMap<>(hashMap);
		}
	}

	@Override
	public void setContextMap(Map<String, String> contextMap) {
		lastOperation.set(WRITE_OPERATION);

		Map<String, String> newMap = Collections.synchronizedMap(new HashMap<>());
		if (contextMap != null) {
			newMap.putAll(contextMap);
		}

		copyOnInheritThreadLocal.set(newMap);
	}

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

	@Override
	public Deque<String> getCopyOfDequeByKey(String key) {
		Map<String, Deque<String>> dequeMap = dequeThreadLocal.get();
		if (dequeMap == null || key == null || dequeMap.get(key) == null) {
			return null;
		}
		return new ArrayDeque<>(dequeMap.get(key));
	}

	@Override
	public void clearDequeByKey(String key) {
		Map<String, Deque<String>> dequeMap = dequeThreadLocal.get();
		if (dequeMap != null && key != null) {
			dequeMap.remove(key);
		}
	}
}
