package com.flightpathfinder.rag.controller.vo;

import java.util.List;

/**
 * 面向 RAG 结构概览的展示对象。
 *
 * @param feature 功能域名称
 * @param module 所属模块
 * @param status 当前实现状态
 * @param mcpToolCount 当前已接入的 MCP 工具数量
 * @param boundaries 当前结构边界说明列表
 */
public record RagOverviewVO(String feature, String module, String status, int mcpToolCount, List<String> boundaries) {
}

