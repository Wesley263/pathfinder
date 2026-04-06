package com.flightpathfinder.admin.service;

/**
 * 管理端辅助诊断查询服务。
 *
 * <p>这些查询用于核验、排障与运维巡检，
 * 不属于终端用户产品流程。
 */
public interface AdminDiagnosticsService {

    /**
        * 执行直飞诊断检查。
     *
     * @param query structured diagnostic query for direct flight search
     * @return admin-facing flight diagnostic result
     */
    AdminFlightDiagnosticResult searchFlights(AdminFlightDiagnosticQuery query);

    /**
        * 执行图路径诊断检查。
     *
     * @param query structured diagnostic query for graph path search
     * @return admin-facing path diagnostic result
     */
    AdminPathDiagnosticResult searchPaths(AdminPathDiagnosticQuery query);

    /**
        * 从运维数据集中查询单个机场。
     *
     * @param iataCode airport IATA code to inspect
     * @return admin-facing airport lookup result
     */
    AdminAirportLookupResult lookupAirport(String iataCode);
}
