package com.flightpathfinder.flight.service;

import com.flightpathfinder.flight.controller.vo.FeatureStructureVO;

/**
 * 航班功能结构查询服务。
 */
public interface FlightStructureService {

    /**
     * 获取当前航班功能结构。
     *
     * @return 航班功能结构视图
     */
    FeatureStructureVO currentStructure();
}

