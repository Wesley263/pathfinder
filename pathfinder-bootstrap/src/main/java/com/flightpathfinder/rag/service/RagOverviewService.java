package com.flightpathfinder.rag.service;

import com.flightpathfinder.rag.controller.vo.RagOverviewVO;

/**
 * 当前 RAG 结构概览的只读服务。
 *
 * <p>它为结构说明类接口提供只读视图，让 controller 不需要直接接触 MCP registry 等底层细节。</p>
 */
public interface RagOverviewService {

    /**
     * 返回当前已实现的 RAG 结构概览。
     *
     * @return 面向架构/运维查看的概览对象
     */
    RagOverviewVO currentOverview();
}
