package com.flightpathfinder.infra.ai.model;

import java.util.Optional;

/**
 * Resolves the currently selected model for one AI capability.
 */
public interface ModelRoutingService {

    /**
     * Selects the active model configuration for the requested capability.
     *
     * @param capability requested AI capability
     * @return resolved model route when configuration is available
     */
    Optional<ModelRoute> selectPrimary(ModelCapability capability);
}
