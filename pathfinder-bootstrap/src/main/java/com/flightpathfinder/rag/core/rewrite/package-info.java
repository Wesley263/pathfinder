/**
 * 承载 RAG 主链中的问题改写相关组件。
 *
 * <p>这一层只负责术语归一、追问补全、子问题拆分等输入整理工作，不直接决定 KB/MCP 分流，
 * 也不介入 retrieval 或 answer 阶段。</p>
 */
package com.flightpathfinder.rag.core.rewrite;
