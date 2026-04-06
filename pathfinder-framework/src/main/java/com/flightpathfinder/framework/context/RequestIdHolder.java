package com.flightpathfinder.framework.context;

import java.util.Optional;
import java.util.UUID;

/**
 * 请求标识线程上下文持有器。
 *
 * <p>用于在一次请求处理链路内透传 requestId，便于日志关联与统一结果回写。
 */
public final class RequestIdHolder {

    /** 当前线程绑定的请求标识。 */
    private static final ThreadLocal<String> REQUEST_ID_HOLDER = new ThreadLocal<>();

    private RequestIdHolder() {
    }

    /**
     * 读取当前线程中的 requestId。
     *
     * @return 当前 requestId（若不存在则为空）
     */
    public static Optional<String> current() {
        return Optional.ofNullable(REQUEST_ID_HOLDER.get());
    }

    /**
     * 获取当前 requestId，不存在时自动创建并写回线程上下文。
     *
     * @return 当前有效 requestId
     */
    public static String getOrCreate() {
        String requestId = REQUEST_ID_HOLDER.get();
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
            REQUEST_ID_HOLDER.set(requestId);
        }
        return requestId;
    }

    /**
     * 写入当前线程 requestId。
     *
     * @param requestId 待写入的请求标识；为空时会清理上下文
     */
    public static void set(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            REQUEST_ID_HOLDER.remove();
            return;
        }
        REQUEST_ID_HOLDER.set(requestId);
    }

    /** 清理当前线程 requestId。 */
    public static void clear() {
        REQUEST_ID_HOLDER.remove();
    }
}

