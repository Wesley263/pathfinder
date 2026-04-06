package com.flightpathfinder.admin.service;

/**
 * 管理端辅助诊断查询服务。
 *
 * 用于定义当前类型或方法在模块内的职责边界。
 * 不属于终端用户产品流程。
 */
public interface AdminDiagnosticsService {

    /**
        * 执行直飞诊断检查。
     *
     * @param query 参数说明。
     * @return 返回结果。
     */
    AdminFlightDiagnosticResult searchFlights(AdminFlightDiagnosticQuery query);

    /**
        * 执行图路径诊断检查。
     *
     * @param query 参数说明。
     * @return 返回结果。
     */
    AdminPathDiagnosticResult searchPaths(AdminPathDiagnosticQuery query);

    /**
        * 从运维数据集中查询单个机场。
     *
     * @param iataCode 参数说明。
     * @return 返回结果。
     */
    AdminAirportLookupResult lookupAirport(String iataCode);
}


