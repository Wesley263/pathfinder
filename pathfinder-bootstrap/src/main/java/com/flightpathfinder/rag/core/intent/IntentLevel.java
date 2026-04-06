package com.flightpathfinder.rag.core.intent;

/**
 * 意图树中的层级类型。
 *
 * 用于定义当前类型或方法在模块内的职责边界。
 */
public enum IntentLevel {
    /** 根领域层。 */
    DOMAIN,
    /** 中间分类层。 */
    CATEGORY,
    /** 可直接参与分类与分流的主题层。 */
    TOPIC
}


