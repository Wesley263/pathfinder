package com.flightpathfinder.rag.core.intent;

import java.util.List;
import java.util.Optional;

/**
 * 意图树只读访问边界。
 *
 * <p>分类器和 resolver 都依赖这套树结构，但只通过接口读取，避免意图定义和分类逻辑彼此耦死。</p>
 */
public interface IntentTree {

    /**
     * 返回整棵意图树的根节点。
     *
     * @return 根节点
     */
    IntentNode root();

    /**
     * 返回所有可直接参与分类的叶子节点。
     *
     * @return 叶子节点列表
     */
    List<IntentNode> leafNodes();

    /**
     * 根据节点标识查找叶子节点。
     *
     * @param nodeId 节点 id、别名或可映射标识
     * @return 查找到的叶子节点
     */
    Optional<IntentNode> findLeafNode(String nodeId);
}
