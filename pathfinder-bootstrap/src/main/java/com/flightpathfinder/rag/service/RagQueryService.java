package com.flightpathfinder.rag.service;

import com.flightpathfinder.rag.service.model.RagQueryCommand;
import com.flightpathfinder.rag.service.model.RagQueryResult;

/**
 * Rag 同步查询服务抽象。
 *
 * 对外提供一次性问答编排入口。
 */
public interface RagQueryService {

    /**
     * 执行同步问答查询。
     *
     * @param command 查询命令
     * @return 查询编排总结果
     */
    RagQueryResult query(RagQueryCommand command);
}


