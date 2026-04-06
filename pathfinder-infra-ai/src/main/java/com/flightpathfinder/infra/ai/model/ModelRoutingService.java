package com.flightpathfinder.infra.ai.model;

import java.util.Optional;

/**
 * 解析单个 AI 能力当前选中的模型路由。
 */
public interface ModelRoutingService {

    /**
     * 为请求能力选择当前生效的模型配置。
     *
     * @param capability requested AI capability
     * @return resolved model route when configuration is available
     */
    Optional<ModelRoute> selectPrimary(ModelCapability capability);
}
