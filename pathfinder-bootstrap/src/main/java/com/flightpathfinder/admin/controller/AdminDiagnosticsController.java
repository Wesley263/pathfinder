package com.flightpathfinder.admin.controller;

import com.flightpathfinder.admin.controller.request.AdminFlightDiagnosticRequest;
import com.flightpathfinder.admin.controller.request.AdminPathDiagnosticRequest;
import com.flightpathfinder.admin.controller.vo.AdminAirportLookupVO;
import com.flightpathfinder.admin.controller.vo.AdminAirportSummaryVO;
import com.flightpathfinder.admin.controller.vo.AdminFlightDiagnosticOptionVO;
import com.flightpathfinder.admin.controller.vo.AdminFlightDiagnosticVO;
import com.flightpathfinder.admin.controller.vo.AdminPathDiagnosticCandidateVO;
import com.flightpathfinder.admin.controller.vo.AdminPathDiagnosticLegVO;
import com.flightpathfinder.admin.controller.vo.AdminPathDiagnosticVO;
import com.flightpathfinder.admin.service.AdminAirportLookupResult;
import com.flightpathfinder.admin.service.AdminAirportSummary;
import com.flightpathfinder.admin.service.AdminDiagnosticsService;
import com.flightpathfinder.admin.service.AdminFlightDiagnosticOption;
import com.flightpathfinder.admin.service.AdminFlightDiagnosticQuery;
import com.flightpathfinder.admin.service.AdminFlightDiagnosticResult;
import com.flightpathfinder.admin.service.AdminPathDiagnosticCandidate;
import com.flightpathfinder.admin.service.AdminPathDiagnosticLeg;
import com.flightpathfinder.admin.service.AdminPathDiagnosticQuery;
import com.flightpathfinder.admin.service.AdminPathDiagnosticResult;
import com.flightpathfinder.framework.convention.Result;
import com.flightpathfinder.framework.web.Results;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端诊断查询 API。
 *
 * <p>这些端点用于运维与研发的直接诊断，暴露原始诊断行为与结构化工具结果，
 * 因此不纳入用户侧搜索 API。
 */
@RestController
@RequestMapping("/api/admin/diagnostics")
public class AdminDiagnosticsController {

    private final AdminDiagnosticsService adminDiagnosticsService;

    public AdminDiagnosticsController(AdminDiagnosticsService adminDiagnosticsService) {
        this.adminDiagnosticsService = adminDiagnosticsService;
    }

    /**
        * 执行直飞诊断查询。
     *
     * @param request admin flight diagnostic request
     * @return diagnostic result adapted for admin inspection
     */
    @PostMapping("/flights")
    public Result<AdminFlightDiagnosticVO> flights(@RequestBody AdminFlightDiagnosticRequest request) {
        AdminFlightDiagnosticResult result = adminDiagnosticsService.searchFlights(new AdminFlightDiagnosticQuery(
                request.origin(),
                request.destination(),
                request.date(),
                request.flexibilityDays(),
                request.topK()));
        return Results.success(toFlightVO(result));
    }

    /**
        * 执行路径诊断查询。
     *
     * @param request admin path diagnostic request
     * @return diagnostic result adapted for admin inspection
     */
    @PostMapping("/paths")
    public Result<AdminPathDiagnosticVO> paths(@RequestBody AdminPathDiagnosticRequest request) {
        AdminPathDiagnosticResult result = adminDiagnosticsService.searchPaths(new AdminPathDiagnosticQuery(
                request.graphKey(),
                request.origin(),
                request.destination(),
                request.maxBudget(),
                request.stopoverDays(),
                request.maxSegments(),
                request.topK()));
        return Results.success(toPathVO(result));
    }

    /**
        * 查询单个机场用于运维核验。
     *
     * @param iata airport IATA code to inspect
     * @return admin-facing airport lookup result
     */
    @GetMapping("/airports/{iata}")
    public Result<AdminAirportLookupVO> airport(@PathVariable String iata) {
        return Results.success(toAirportVO(adminDiagnosticsService.lookupAirport(iata)));
    }

    private AdminFlightDiagnosticVO toFlightVO(AdminFlightDiagnosticResult result) {
        return new AdminFlightDiagnosticVO(
                result.toolId(),
                result.toolAvailable(),
                result.success(),
                result.status(),
                result.message(),
                result.error(),
                result.retryable(),
                result.suggestedAction(),
                result.origin(),
                result.destination(),
                result.date(),
                result.flexibilityDays(),
                result.topK(),
                result.flightCount(),
                result.checkedAt(),
                result.flights().stream().map(this::toFlightOptionVO).toList());
    }

    private AdminFlightDiagnosticOptionVO toFlightOptionVO(AdminFlightDiagnosticOption option) {
        return new AdminFlightDiagnosticOptionVO(
                option.airlineCode(),
                option.airlineName(),
                option.airlineType(),
                option.origin(),
                option.destination(),
                option.date(),
                option.priceCny(),
                option.basePriceCny(),
                option.durationMinutes(),
                option.distanceKm(),
                option.lowCostCarrier());
    }

    private AdminPathDiagnosticVO toPathVO(AdminPathDiagnosticResult result) {
        return new AdminPathDiagnosticVO(
                result.toolId(),
                result.toolAvailable(),
                result.success(),
                result.status(),
                result.message(),
                result.error(),
                result.snapshotMiss(),
                result.retryable(),
                result.suggestedAction(),
                result.graphKey(),
                result.snapshotVersion(),
                result.schemaVersion(),
                result.origin(),
                result.destination(),
                result.maxBudget(),
                result.stopoverDays(),
                result.maxSegments(),
                result.topK(),
                result.pathCount(),
                result.checkedAt(),
                result.paths().stream().map(this::toPathCandidateVO).toList());
    }

    private AdminPathDiagnosticCandidateVO toPathCandidateVO(AdminPathDiagnosticCandidate candidate) {
        return new AdminPathDiagnosticCandidateVO(
                candidate.segmentCount(),
                candidate.transferCount(),
                candidate.totalPriceCny(),
                candidate.totalDurationMinutes(),
                candidate.totalDistanceKm(),
                candidate.averageOnTimeRate(),
                candidate.hubAirports(),
                candidate.legs().stream().map(this::toPathLegVO).toList());
    }

    private AdminPathDiagnosticLegVO toPathLegVO(AdminPathDiagnosticLeg leg) {
        return new AdminPathDiagnosticLegVO(
                leg.origin(),
                leg.destination(),
                leg.carrierCode(),
                leg.carrierName(),
                leg.carrierType(),
                leg.priceCny(),
                leg.durationMinutes(),
                leg.distanceKm());
    }

    private AdminAirportLookupVO toAirportVO(AdminAirportLookupResult result) {
        return new AdminAirportLookupVO(
                result.iataCode(),
                result.status(),
                result.message(),
                result.checkedAt(),
                result.airport() == null ? null : toAirportSummaryVO(result.airport()));
    }

    private AdminAirportSummaryVO toAirportSummaryVO(AdminAirportSummary airport) {
        return new AdminAirportSummaryVO(
                airport.iataCode(),
                airport.icaoCode(),
                airport.nameEn(),
                airport.nameCn(),
                airport.city(),
                airport.cityCn(),
                airport.countryCode(),
                airport.latitude(),
                airport.longitude(),
                airport.timezone(),
                airport.type(),
                airport.source(),
                airport.outgoingRouteCount(),
                airport.incomingRouteCount(),
                airport.totalRouteCount(),
                airport.currentSnapshotStatus(),
                airport.presentInCurrentSnapshot(),
                airport.currentSnapshotVersion(),
                airport.observedAt());
    }
}
