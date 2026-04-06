package com.flightpathfinder.framework.readmodel.graph;

/**
 * 图快照 schema 版本常量与兼容性判断。
 */
public final class GraphSnapshotVersions {

    /** 当前支持的 schema 版本。 */
    public static final String CURRENT_SCHEMA_VERSION = "1.0";

    /** 工具类禁止实例化。 */
    private GraphSnapshotVersions() {
    }

    /**
     * 判断给定 schema 版本是否受支持。
     *
     * @param schemaVersion 待判断版本
     * @return 支持返回 true
     */
    public static boolean supports(String schemaVersion) {
        return CURRENT_SCHEMA_VERSION.equals(schemaVersion);
    }
}
