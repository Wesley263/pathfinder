package com.flightpathfinder.framework.trace;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

/**
 * 追踪根节点线程上下文持有器。
 *
 * <p>负责在一次请求生命周期内创建、读取与清理当前 TraceRoot。
 */
public final class TraceContext {

    /** 当前线程绑定的追踪根节点。 */
    private static final ThreadLocal<TraceRoot> ROOT_HOLDER = new ThreadLocal<>();

    private TraceContext() {
    }

    /**
     * 启动新的追踪根节点并绑定到当前线程。
     *
     * @param scene 追踪场景标识
     * @return 新创建的追踪根节点
     */
    public static TraceRoot startRoot(String scene) {
        TraceRoot root = new TraceRoot(UUID.randomUUID().toString(), scene, Instant.now(), new ArrayList<>());
        ROOT_HOLDER.set(root);
        return root;
    }

    /**
     * 读取当前线程追踪根节点。
     *
     * @return 当前 TraceRoot（若不存在则为空）
     */
    public static Optional<TraceRoot> currentRoot() {
        return Optional.ofNullable(ROOT_HOLDER.get());
    }

    /** 清理当前线程追踪上下文。 */
    public static void clear() {
        ROOT_HOLDER.remove();
    }
}

