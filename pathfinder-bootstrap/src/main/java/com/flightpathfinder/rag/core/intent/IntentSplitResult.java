package com.flightpathfinder.rag.core.intent;

import java.util.List;

public record IntentSplitResult(
        List<ResolvedIntent> kbIntents,
        List<ResolvedIntent> mcpIntents,
        List<ResolvedIntent> systemIntents) {

    public IntentSplitResult {
        kbIntents = List.copyOf(kbIntents == null ? List.of() : kbIntents);
        mcpIntents = List.copyOf(mcpIntents == null ? List.of() : mcpIntents);
        systemIntents = List.copyOf(systemIntents == null ? List.of() : systemIntents);
    }

    public static IntentSplitResult empty() {
        return new IntentSplitResult(List.of(), List.of(), List.of());
    }
}
