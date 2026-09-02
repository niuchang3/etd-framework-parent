package org.etd.framework.starter.mybaits.permission.context;

import org.etd.framework.starter.mybaits.permission.annotation.DataPermission;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 数据权限注解上下文持有者
 * <p>
 * 维护当前线程栈中的 @DataPermission 注解元数据。
 *
 * @author 牛昌
 */
public class DataPermissionContextHolder {

    private static final ThreadLocal<Deque<DataPermission>> DATA_PERMISSION_STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    private DataPermissionContextHolder() {
    }

    /**
     * 将数据权限注解元数据压入当前线程栈
     *
     * @param dataPermission 数据权限注解
     */
    public static void push(DataPermission dataPermission) {
        DATA_PERMISSION_STACK.get().push(dataPermission);
    }

    /**
     * 弹出当前线程栈顶的数据权限注解
     *
     * @return 数据权限注解，不存在时返回 null
     */
    public static DataPermission pop() {
        Deque<DataPermission> deque = DATA_PERMISSION_STACK.get();
        if (deque.isEmpty()) {
            return null;
        }
        DataPermission popped = deque.pop();
        if (deque.isEmpty()) {
            DATA_PERMISSION_STACK.remove();
        }
        return popped;
    }

    /**
     * 获取当前线程栈顶的数据权限注解
     *
     * @return 数据权限注解，不存在时返回 null
     */
    public static DataPermission peek() {
        Deque<DataPermission> deque = DATA_PERMISSION_STACK.get();
        return deque.isEmpty() ? null : deque.peek();
    }

    /**
     * 清理当前线程上下文
     */
    public static void clear() {
        DATA_PERMISSION_STACK.remove();
    }
}
