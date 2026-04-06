package com.flightpathfinder.rag.core.intent;

import java.util.List;

/**
 * 说明。
 *
 * 说明。
 * 说明。
 *
 * @param kbIntents 命中的知识检索意图列表
 * @param mcpIntents 参数说明。
 * @param systemIntents 命中的系统级意图列表
 */
public record IntentSplitResult(
        List<ResolvedIntent> kbIntents,
        List<ResolvedIntent> mcpIntents,
        List<ResolvedIntent> systemIntents) {

    /**
     * 归一化意图分流结果。
     */
    public IntentSplitResult {
        kbIntents = List.copyOf(kbIntents == null ? List.of() : kbIntents);
        mcpIntents = List.copyOf(mcpIntents == null ? List.of() : mcpIntents);
        systemIntents = List.copyOf(systemIntents == null ? List.of() : systemIntents);
    }

    /**
     * 创建空的分流结果。
     *
     * @return 三路均为空的结果对象
     */
    public static IntentSplitResult empty() {
        return new IntentSplitResult(List.of(), List.of(), List.of());
    }
}
