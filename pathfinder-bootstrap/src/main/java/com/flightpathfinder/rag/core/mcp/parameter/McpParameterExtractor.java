package com.flightpathfinder.rag.core.mcp.parameter;

import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import com.flightpathfinder.rag.core.intent.ResolvedIntent;
import com.flightpathfinder.rag.core.rewrite.RewriteResult;

public interface McpParameterExtractor {

    McpParameterExtractionResult extract(RewriteResult rewriteResult, ResolvedIntent resolvedIntent, McpToolDescriptor toolDescriptor);
}
