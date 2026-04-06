/**
 * 承载 RAG 第一阶段的编排边界。
 *
 * <p>这一层只负责问题改写、意图识别和 KB/MCP 分流结果的交接，不在这里掺入检索、
 * 工具调用或最终回答生成逻辑，这样后续阶段的职责边界才能保持清晰。</p>
 */
package com.flightpathfinder.rag.core.pipeline;
