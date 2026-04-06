package com.flightpathfinder.rag.core.memory;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 会话记忆模块配置项。
 *
 * <p>该配置用于控制近期轮次窗口、摘要开关以及摘要触发阈值。
 * 放在 memory 层可让主链编排只依赖能力接口，而不直接绑定策略细节。
 */
@Component
@ConfigurationProperties(prefix = "pathfinder.rag.memory")
public class ConversationMemoryProperties {

    /** 记忆上下文默认保留的近期轮次数。 */
    private int recentTurnLimit = 4;
    /** 是否启用摘要压缩能力。 */
    private boolean summaryEnabled = true;
    /** 从多少轮开始允许触发摘要。 */
    private int summaryStartTurns = 6;
    /** 摘要文本最大字符数。 */
    private int summaryMaxChars = 360;

    /**
     * 获取近期轮次保留上限。
     */
    public int recentTurnLimit() {
        return recentTurnLimit;
    }

    /**
     * 设置近期轮次保留上限，最小为 1。
     */
    public void setRecentTurnLimit(int recentTurnLimit) {
        this.recentTurnLimit = Math.max(1, recentTurnLimit);
    }

    /**
     * 获取摘要开关状态。
     */
    public boolean summaryEnabled() {
        return summaryEnabled;
    }

    /**
     * 设置摘要开关状态。
     */
    public void setSummaryEnabled(boolean summaryEnabled) {
        this.summaryEnabled = summaryEnabled;
    }

    /**
     * 获取摘要触发最小轮次。
     */
    public int summaryStartTurns() {
        return summaryStartTurns;
    }

    /**
     * 设置摘要触发最小轮次，最小为 1。
     */
    public void setSummaryStartTurns(int summaryStartTurns) {
        this.summaryStartTurns = Math.max(1, summaryStartTurns);
    }

    /**
     * 获取摘要最大字符数。
     */
    public int summaryMaxChars() {
        return summaryMaxChars;
    }

    /**
     * 设置摘要最大字符数，最小为 120。
     */
    public void setSummaryMaxChars(int summaryMaxChars) {
        this.summaryMaxChars = Math.max(120, summaryMaxChars);
    }
}
