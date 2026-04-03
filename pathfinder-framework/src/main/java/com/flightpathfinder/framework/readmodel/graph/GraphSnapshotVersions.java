package com.flightpathfinder.framework.readmodel.graph;

public final class GraphSnapshotVersions {

    public static final String CURRENT_SCHEMA_VERSION = "1.0";

    private GraphSnapshotVersions() {
    }

    public static boolean supports(String schemaVersion) {
        return CURRENT_SCHEMA_VERSION.equals(schemaVersion);
    }
}
