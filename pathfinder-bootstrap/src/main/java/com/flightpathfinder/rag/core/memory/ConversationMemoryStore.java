package com.flightpathfinder.rag.core.memory;

/**
 * 面向持久化层的近期轮次记忆存储抽象。
 *
 * 用于定义当前类型或方法在模块内的职责边界。
 * 这种拆分让原始历史读取保持简单，也使摘要策略可以独立演进而不影响写路径存储契约。
 */
public interface ConversationMemoryStore {

    /**
     * 加载指定会话的近期轮次快照。
     *
     * @param conversationId 稳定会话标识
     * @param recentTurnLimit 调用方请求的近期轮次数量
     * @return 含会话元信息与近期轮次的快照；无历史时返回空快照
     */
    ConversationMemorySnapshot load(String conversationId, int recentTurnLimit);

    /**
     * 追加写入一轮完整的用户/助手对话。
     *
     * @param conversationId 稳定会话标识
     * @param turn 最终答案产出后组装完成的归一化轮次
     */
    void appendTurn(String conversationId, ConversationMemoryTurn turn);
}


