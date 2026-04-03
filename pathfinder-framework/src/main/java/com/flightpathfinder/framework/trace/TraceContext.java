package com.flightpathfinder.framework.trace;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

public final class TraceContext {

    private static final ThreadLocal<TraceRoot> ROOT_HOLDER = new ThreadLocal<>();

    private TraceContext() {
    }

    public static TraceRoot startRoot(String scene) {
        TraceRoot root = new TraceRoot(UUID.randomUUID().toString(), scene, Instant.now(), new ArrayList<>());
        ROOT_HOLDER.set(root);
        return root;
    }

    public static Optional<TraceRoot> currentRoot() {
        return Optional.ofNullable(ROOT_HOLDER.get());
    }

    public static void clear() {
        ROOT_HOLDER.remove();
    }
}

