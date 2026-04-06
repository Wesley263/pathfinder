package com.flightpathfinder.rag.core.intent;

import java.util.List;
import java.util.Optional;

/**
 * 意图树只读访问边界。
 *
 * 说明。
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
     * @param nodeId 参数说明。
     * @return 查找到的叶子节点
     */
    Optional<IntentNode> findLeafNode(String nodeId);
}
