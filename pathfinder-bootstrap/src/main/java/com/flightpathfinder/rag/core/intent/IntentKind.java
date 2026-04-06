package com.flightpathfinder.rag.core.intent;

/**
 * 意图所属的大类。
 *
 * <p>MCP 表示需要走工具调用，KB 表示需要走知识检索，SYSTEM 表示闲聊或超出当前业务域的问题。</p>
 */
public enum IntentKind {
    /** 需要进入 MCP 工具执行分支。 */
    MCP,
    /** 需要进入知识检索分支。 */
    KB,
    /** 需要进入系统级兜底或通用助手分支。 */
    SYSTEM
}
