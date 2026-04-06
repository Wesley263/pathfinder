package com.flightpathfinder.rag.controller.vo;

import java.util.List;

/**
 * 用于定义当前类型或方法在模块内的职责边界。
 *
 * @param feature 功能域名称
 * @param module 所属模块
 * @param status 当前实现状态
 * @param mcpToolCount 参数说明。
 * @param boundaries 当前结构边界说明列表
 */
public record RagOverviewVO(String feature, String module, String status, int mcpToolCount, List<String> boundaries) {
}



