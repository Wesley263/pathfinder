package com.flightpathfinder.framework.context;

import java.util.Optional;
import java.util.UUID;

/**
 * 请求标识线程上下文持有器。
 *
 * 说明。
 */
public final class RequestIdHolder {

    /** 当前线程绑定的请求标识。 */
    private static final ThreadLocal<String> REQUEST_ID_HOLDER = new ThreadLocal<>();

    private RequestIdHolder() {
    }

    /**
     * 说明。
     *
     * @return 返回结果。
     */
    public static Optional<String> current() {
        return Optional.ofNullable(REQUEST_ID_HOLDER.get());
    }

    /**
     * 说明。
     *
     * @return 返回结果。
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
     * 说明。
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

    /** 注释说明。 */
    public static void clear() {
        REQUEST_ID_HOLDER.remove();
    }
}

