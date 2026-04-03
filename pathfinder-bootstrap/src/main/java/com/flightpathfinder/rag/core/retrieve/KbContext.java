package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.rag.core.intent.ResolvedIntent;
import java.util.List;

public record KbContext(
        String status,
        String summary,
        List<ResolvedIntent> matchedIntents,
        List<KbRetrievalItem> items,
        boolean empty,
        String error) {

    public KbContext {
        status = status == null || status.isBlank() ? "EMPTY" : status;
        summary = summary == null ? "" : summary.trim();
        matchedIntents = List.copyOf(matchedIntents == null ? List.of() : matchedIntents);
        items = List.copyOf(items == null ? List.of() : items);
        error = error == null ? "" : error.trim();
        empty = empty || items.isEmpty();
    }

    public boolean hasError() {
        return "ERROR".equals(status);
    }

    public static KbContext empty(String summary, List<ResolvedIntent> matchedIntents) {
        return new KbContext("EMPTY", summary, matchedIntents, List.of(), true, "");
    }

    public static KbContext error(String summary, List<ResolvedIntent> matchedIntents, String error) {
        return new KbContext("ERROR", summary, matchedIntents, List.of(), true, error);
    }
}
