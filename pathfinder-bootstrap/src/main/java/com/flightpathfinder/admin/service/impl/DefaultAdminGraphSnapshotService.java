package com.flightpathfinder.admin.service.impl;

import com.flightpathfinder.admin.service.AdminGraphSnapshotService;
import com.flightpathfinder.admin.service.AdminGraphSnapshotStatus;
import com.flightpathfinder.flight.config.GraphSnapshotProperties;
import com.flightpathfinder.flight.core.graph.snapshot.GraphSnapshotLifecycleService;
import com.flightpathfinder.flight.core.graph.snapshot.GraphSnapshotQueryService;
import com.flightpathfinder.framework.errorcode.BaseErrorCode;
import com.flightpathfinder.framework.exception.AbstractException;
import com.flightpathfinder.framework.exception.ServiceException;
import com.flightpathfinder.framework.readmodel.graph.GraphSnapshot;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Default admin service for graph snapshot inspection and lifecycle control.
 *
 * <p>This service belongs to the graph admin surface because snapshot state is an operational read-model
 * concern. It reuses graph lifecycle/query boundaries instead of duplicating Redis logic in controllers.
 */
@Service
public class DefaultAdminGraphSnapshotService implements AdminGraphSnapshotService {

    private final GraphSnapshotLifecycleService graphSnapshotLifecycleService;
    private final GraphSnapshotQueryService graphSnapshotQueryService;
    private final GraphSnapshotProperties graphSnapshotProperties;

    public DefaultAdminGraphSnapshotService(GraphSnapshotLifecycleService graphSnapshotLifecycleService,
                                            GraphSnapshotQueryService graphSnapshotQueryService,
                                            GraphSnapshotProperties graphSnapshotProperties) {
        this.graphSnapshotLifecycleService = graphSnapshotLifecycleService;
        this.graphSnapshotQueryService = graphSnapshotQueryService;
        this.graphSnapshotProperties = graphSnapshotProperties;
    }

    /**
     * Loads the current published snapshot status for the requested graph key.
     *
     * @param graphKey logical graph identifier; blank means the default graph
     * @return snapshot status or structured {@code SNAPSHOT_MISS} view
     */
    @Override
    public AdminGraphSnapshotStatus currentSnapshot(String graphKey) {
        String resolvedGraphKey = resolveGraphKey(graphKey);
        try {
            return graphSnapshotQueryService.loadCurrent(resolvedGraphKey)
                    .map(snapshot -> toStatus(snapshot, "READY", "current snapshot loaded"))
                    .orElseGet(() -> new AdminGraphSnapshotStatus(
                            resolvedGraphKey,
                            "SNAPSHOT_MISS",
                            null,
                            null,
                            null,
                            Instant.now(),
                            0,
                            0,
                            null,
                            "graph snapshot is not available for graphKey=" + resolvedGraphKey,
                            Map.of()));
        } catch (AbstractException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ServiceException(
                    BaseErrorCode.SERVICE_ERROR,
                    "failed to query current graph snapshot for graphKey=" + resolvedGraphKey + ": " + exception.getMessage());
        }
    }

    /**
     * Rebuilds and republishes the graph snapshot.
     *
     * @param graphKey logical graph identifier; blank means the default graph
     * @param reason operator-supplied rebuild reason
     * @return rebuilt snapshot status
     */
    @Override
    public AdminGraphSnapshotStatus rebuild(String graphKey, String reason) {
        String resolvedGraphKey = resolveGraphKey(graphKey);
        String resolvedReason = normalizeReason(reason, "manual admin rebuild");
        try {
            GraphSnapshot snapshot = graphSnapshotLifecycleService.rebuild(resolvedGraphKey, resolvedReason);
            return toStatus(snapshot, "REBUILT", resolvedReason);
        } catch (AbstractException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ServiceException(
                    BaseErrorCode.SERVICE_ERROR,
                    "failed to rebuild graph snapshot for graphKey=" + resolvedGraphKey + ": " + exception.getMessage());
        }
    }

    /**
     * Invalidates the currently published snapshot without rebuilding it.
     *
     * @param graphKey logical graph identifier; blank means the default graph
     * @param reason operator-supplied invalidation reason
     * @return invalidation status summary
     */
    @Override
    public AdminGraphSnapshotStatus invalidate(String graphKey, String reason) {
        String resolvedGraphKey = resolveGraphKey(graphKey);
        String resolvedReason = normalizeReason(reason, "manual admin invalidate");
        try {
            graphSnapshotLifecycleService.invalidate(resolvedGraphKey);
            return new AdminGraphSnapshotStatus(
                    resolvedGraphKey,
                    "INVALIDATED",
                    null,
                    null,
                    null,
                    Instant.now(),
                    0,
                    0,
                    null,
                    resolvedReason,
                    Map.of());
        } catch (AbstractException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ServiceException(
                    BaseErrorCode.SERVICE_ERROR,
                    "failed to invalidate graph snapshot for graphKey=" + resolvedGraphKey + ": " + exception.getMessage());
        }
    }

    private AdminGraphSnapshotStatus toStatus(GraphSnapshot snapshot, String status, String reason) {
        return new AdminGraphSnapshotStatus(
                snapshot.graphKey(),
                status,
                snapshot.snapshotVersion(),
                snapshot.schemaVersion(),
                snapshot.generatedAt(),
                Instant.now(),
                snapshot.nodes().size(),
                snapshot.edges().size(),
                snapshot.sourceFingerprint(),
                reason,
                snapshot.metadata() == null ? Map.of() : snapshot.metadata());
    }

    private String resolveGraphKey(String graphKey) {
        return graphKey == null || graphKey.isBlank()
                ? graphSnapshotProperties.getDefaultGraphKey()
                : graphKey.trim();
    }

    private String normalizeReason(String reason, String defaultValue) {
        return reason == null || reason.isBlank()
                ? defaultValue
                : reason.trim();
    }
}
