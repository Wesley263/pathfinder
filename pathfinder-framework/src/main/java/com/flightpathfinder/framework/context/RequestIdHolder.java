package com.flightpathfinder.framework.context;

import java.util.Optional;
import java.util.UUID;

public final class RequestIdHolder {

    private static final ThreadLocal<String> REQUEST_ID_HOLDER = new ThreadLocal<>();

    private RequestIdHolder() {
    }

    public static Optional<String> current() {
        return Optional.ofNullable(REQUEST_ID_HOLDER.get());
    }

    public static String getOrCreate() {
        String requestId = REQUEST_ID_HOLDER.get();
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
            REQUEST_ID_HOLDER.set(requestId);
        }
        return requestId;
    }

    public static void set(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            REQUEST_ID_HOLDER.remove();
            return;
        }
        REQUEST_ID_HOLDER.set(requestId);
    }

    public static void clear() {
        REQUEST_ID_HOLDER.remove();
    }
}

