package com.flightpathfinder.framework.trace;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

/**
 * 追踪根节点线程上下文持有器。
 *
 * 用于定义当前类型或方法在模块内的职责边界。
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
     * @return 返回结果。
     */
    public static Optional<TraceRoot> currentRoot() {
        return Optional.ofNullable(ROOT_HOLDER.get());
    }

    /** 清理当前线程追踪上下文。 */
    public static void clear() {
        ROOT_HOLDER.remove();
    }
}



