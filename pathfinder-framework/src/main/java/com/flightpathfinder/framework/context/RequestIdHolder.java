package com.flightpathfinder.framework.context;

import java.util.Optional;
import java.util.UUID;

/**
 * 请求标识线程上下文持有器。
 *
 * 统一管理请求标识的读取、懒创建与清理，避免业务层直接操作 ThreadLocal。
 */
public final class RequestIdHolder {

    /** 当前线程绑定的请求标识。 */
    private static final ThreadLocal<String> REQUEST_ID_HOLDER = new ThreadLocal<>();

    private RequestIdHolder() {
    }

    /**
     * 获取当前线程绑定的请求标识。
     *
     * @return 已绑定时返回请求标识，否则返回空
     */
    public static Optional<String> current() {
        return Optional.ofNullable(REQUEST_ID_HOLDER.get());
    }

    /**
     * 读取当前请求标识，不存在时自动创建并写回线程上下文。
     *
     * @return 可用于链路追踪的请求标识
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
        * 设置当前线程请求标识。
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

    /** 清理当前线程绑定的请求标识。 */
    public static void clear() {
        REQUEST_ID_HOLDER.remove();
    }
}

