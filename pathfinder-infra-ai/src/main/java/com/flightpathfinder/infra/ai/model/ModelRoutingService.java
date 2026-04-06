package com.flightpathfinder.infra.ai.model;

import java.util.Optional;

/**
 * 模型路由服务。
 *
 * 按能力从配置中选择当前可用的主路由。
 */
public interface ModelRoutingService {

    /**
     * 为请求能力选择当前生效的模型配置。
     *
     * @param capability 请求能力
     * @return 主路由（不存在时为空）
     */
    Optional<ModelRoute> selectPrimary(ModelCapability capability);
}
