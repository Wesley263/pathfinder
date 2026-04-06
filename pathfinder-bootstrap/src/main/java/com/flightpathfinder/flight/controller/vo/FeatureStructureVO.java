package com.flightpathfinder.flight.controller.vo;

import java.util.List;

/**
 * 航班功能结构展示对象。
 *
 * @param feature 功能标识
 * @param module 所属模块
 * @param status 当前状态
 * @param boundaries 边界说明列表
 */
public record FeatureStructureVO(String feature, String module, String status, List<String> boundaries) {
}

