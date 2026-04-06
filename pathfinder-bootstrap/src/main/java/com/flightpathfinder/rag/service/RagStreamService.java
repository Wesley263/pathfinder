package com.flightpathfinder.rag.service;

import com.flightpathfinder.rag.service.model.RagStreamCommand;
import com.flightpathfinder.rag.service.model.RagStreamEventWriter;

/**
 * 流式 user-facing RAG 入口。
 *
 * <p>它复用同步主链的阶段能力，但把阶段进度和回答内容改为 SSE 事件输出，
 * 不再阻塞等待一次性响应。</p>
 */
public interface RagStreamService {

    /**
     * 执行流式 RAG 主链并向事件写入器输出 SSE 事件。
     *
     * @param command 由 HTTP 请求转换而来的流式命令
     * @param eventWriter 接收阶段事件、chunk 事件和完成事件的输出器
     */
    void stream(RagStreamCommand command, RagStreamEventWriter eventWriter);
}
