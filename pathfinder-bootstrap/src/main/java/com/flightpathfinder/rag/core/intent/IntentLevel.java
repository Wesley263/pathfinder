package com.flightpathfinder.rag.core.intent;

/**
 * 意图树中的层级类型。
 *
 * <p>2.0 当前意图树使用领域、分类、主题三级结构，主题节点才是最终可命中的叶子节点。</p>
 */
public enum IntentLevel {
    /** 根领域层。 */
    DOMAIN,
    /** 中间分类层。 */
    CATEGORY,
    /** 可直接参与分类与分流的主题层。 */
    TOPIC
}
