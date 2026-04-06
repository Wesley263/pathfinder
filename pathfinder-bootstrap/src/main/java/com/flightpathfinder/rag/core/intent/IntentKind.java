package com.flightpathfinder.rag.core.intent;

/**
 * 意图所属的大类。
 *
 * 用于驱动后续分支选择：工具执行、知识检索或系统兜底。
 */
public enum IntentKind {
    /** 需要进入 MCP 工具执行分支。 */
    MCP,
    /** 需要进入知识检索分支。 */
    KB,
    /** 需要进入系统级兜底或通用助手分支。 */
    SYSTEM
}

