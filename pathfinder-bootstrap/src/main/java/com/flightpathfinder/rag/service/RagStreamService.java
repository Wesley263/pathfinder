package com.flightpathfinder.rag.service;

import com.flightpathfinder.rag.service.model.RagStreamCommand;
import com.flightpathfinder.rag.service.model.RagStreamEventWriter;

/**
 * Rag 流式查询服务抽象。
 *
 * 对外提供事件流式输出的问答入口。
 */
public interface RagStreamService {

    /**
     * 执行流式问答并持续写出事件。
     *
     * @param command 流式查询命令
     * @param eventWriter 事件写出器
     */
    void stream(RagStreamCommand command, RagStreamEventWriter eventWriter);
}

