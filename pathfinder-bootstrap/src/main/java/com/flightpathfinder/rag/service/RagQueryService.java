package com.flightpathfinder.rag.service;

import com.flightpathfinder.rag.service.model.RagQueryCommand;
import com.flightpathfinder.rag.service.model.RagQueryResult;

/**
 * 同步 user-facing RAG 入口。
 *
 * <p>它负责最顶层请求编排，是 request 级 memory 和 trace 生命周期的正确归属层。
 * 控制器只做协议适配，不直接拼主链。</p>
 */
public interface RagQueryService {

    /**
     * 执行同步 RAG 主链。
     *
     * @param command 由 HTTP 请求转换而来的查询命令
     * @return 包含 stage one、retrieval、answer 和 trace 的完整结果
     */
    RagQueryResult query(RagQueryCommand command);
}

