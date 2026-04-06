package com.flightpathfinder.rag.core.intent;

/**
 * 意图所属的大类。
 *
 * 说明。
 */
public enum IntentKind {
    /** 注释说明。 */
    MCP,
    /** 需要进入知识检索分支。 */
    KB,
    /** 需要进入系统级兜底或通用助手分支。 */
    SYSTEM
}
