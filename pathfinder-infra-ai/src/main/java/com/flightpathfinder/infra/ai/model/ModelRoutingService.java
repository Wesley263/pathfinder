package com.flightpathfinder.infra.ai.model;

import java.util.Optional;

/**
 * 说明。
 */
public interface ModelRoutingService {

    /**
     * 为请求能力选择当前生效的模型配置。
     *
     * @param capability 参数说明。
     * @return 返回结果。
     */
    Optional<ModelRoute> selectPrimary(ModelCapability capability);
}
