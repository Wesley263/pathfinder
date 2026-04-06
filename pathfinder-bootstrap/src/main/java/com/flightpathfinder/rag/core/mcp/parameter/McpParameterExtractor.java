package com.flightpathfinder.rag.core.mcp.parameter;

import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import com.flightpathfinder.rag.core.intent.ResolvedIntent;
import com.flightpathfinder.rag.core.rewrite.RewriteResult;
/**
 * 用于提供当前领域能力的默认实现。
 */
public interface McpParameterExtractor {

    McpParameterExtractionResult extract(RewriteResult rewriteResult, ResolvedIntent resolvedIntent, McpToolDescriptor toolDescriptor);
}



