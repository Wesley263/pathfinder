package com.flightpathfinder.rag.core.intent;

import java.util.List;
import java.util.Optional;

public interface IntentTree {

    IntentNode root();

    List<IntentNode> leafNodes();

    Optional<IntentNode> findLeafNode(String nodeId);
}
