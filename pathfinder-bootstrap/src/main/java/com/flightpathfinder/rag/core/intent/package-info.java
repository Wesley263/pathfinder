/**
 * 承载 RAG 第一阶段中的意图识别与分流组件。
 *
 * <p>这一层负责把 rewrite 产物转换为可审计的意图打分、子问题结果和 KB/MCP/SYSTEM 分流，
 * 但不直接做检索或工具调用。</p>
 */
package com.flightpathfinder.rag.core.intent;
