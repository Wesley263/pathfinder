package com.flightpathfinder.rag.core.memory;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "pathfinder.rag.memory")
public class ConversationMemoryProperties {

    private int recentTurnLimit = 4;
    private boolean summaryEnabled = true;
    private int summaryStartTurns = 6;
    private int summaryMaxChars = 360;

    public int recentTurnLimit() {
        return recentTurnLimit;
    }

    public void setRecentTurnLimit(int recentTurnLimit) {
        this.recentTurnLimit = Math.max(1, recentTurnLimit);
    }

    public boolean summaryEnabled() {
        return summaryEnabled;
    }

    public void setSummaryEnabled(boolean summaryEnabled) {
        this.summaryEnabled = summaryEnabled;
    }

    public int summaryStartTurns() {
        return summaryStartTurns;
    }

    public void setSummaryStartTurns(int summaryStartTurns) {
        this.summaryStartTurns = Math.max(1, summaryStartTurns);
    }

    public int summaryMaxChars() {
        return summaryMaxChars;
    }

    public void setSummaryMaxChars(int summaryMaxChars) {
        this.summaryMaxChars = Math.max(120, summaryMaxChars);
    }
}
