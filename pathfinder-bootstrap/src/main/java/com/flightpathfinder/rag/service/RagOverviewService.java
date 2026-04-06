package com.flightpathfinder.rag.service;

import com.flightpathfinder.rag.controller.vo.RagOverviewVO;

/**
 * Rag 概览查询服务抽象。
 *
 * 提供当前能力接线与运行状态的只读概览。
 */
public interface RagOverviewService {

    /**
     * 获取当前 Rag 概览。
     *
     * @return 面向架构/运维查看的概览对象
     */
    RagOverviewVO currentOverview();
}

