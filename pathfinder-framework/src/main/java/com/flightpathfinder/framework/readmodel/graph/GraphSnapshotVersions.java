package com.flightpathfinder.framework.readmodel.graph;

/**
 * 说明。
 */
public final class GraphSnapshotVersions {

    /** 注释说明。 */
    public static final String CURRENT_SCHEMA_VERSION = "1.0";

    /** 工具类禁止实例化。 */
    private GraphSnapshotVersions() {
    }

    /**
     * 说明。
     *
     * @param schemaVersion 待判断版本
     * @return 返回结果。
     */
    public static boolean supports(String schemaVersion) {
        return CURRENT_SCHEMA_VERSION.equals(schemaVersion);
    }
}
