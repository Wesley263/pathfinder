package com.flightpathfinder.rag.core.intent;

/**
 * 意图树中的层级类型。
 *
 * 说明。
 */
public enum IntentLevel {
    /** 根领域层。 */
    DOMAIN,
    /** 中间分类层。 */
    CATEGORY,
    /** 可直接参与分类与分流的主题层。 */
    TOPIC
}
