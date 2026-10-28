package org.etd.framework.starter.ai.model.client;

import java.util.LinkedHashMap;
import java.util.Map;
import org.etd.framework.common.core.constants.HeaderConstant;
import org.etd.framework.common.core.context.model.RequestContext;

/** 单次模型请求的上下文消息头；供应商认证始终由模型连接控制，不转发用户身份凭证。 */
public final class ModelRequestHeaders {
    /** Reactor订阅上下文中的消息头快照键，避免SDK缓存携带某次用户请求状态。 */
    public static final String REACTOR_KEY = ModelRequestHeaders.class.getName();

    private ModelRequestHeaders() {
    }

    /** @return 当前请求的非认证上下文头；仅在内存使用，不写入共享Builder默认值 */
    public static Map<String, String> capture() {
        Map<String, String> headers = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : RequestContext.getHeaderContext().toMap().entrySet()) {
            String key = entry.getKey();
            if (!HeaderConstant.AUTHORIZATION.equalsIgnoreCase(key)
                    && !HeaderConstant.InternalHeader.X_USER_DETAILS.equalsIgnoreCase(key)) {
                headers.put(key, entry.getValue().toString());
            }
        }
        return Map.copyOf(headers);
    }
}
