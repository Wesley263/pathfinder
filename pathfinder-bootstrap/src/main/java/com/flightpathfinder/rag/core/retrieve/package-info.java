/**
 * 承载 RAG retrieval 阶段的 KB 检索、MCP 执行和结果模型。
 *
 * <p>这一层明确把 KB 和 MCP 两条分支并行保留，再统一收口成 RetrievalResult，
 * 方便后续 final answer 解释证据来源和部分成功语义。</p>
 */
package com.flightpathfinder.rag.core.retrieve;
