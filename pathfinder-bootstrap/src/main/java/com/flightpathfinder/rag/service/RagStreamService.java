package com.flightpathfinder.rag.service;

import com.flightpathfinder.rag.service.model.RagStreamCommand;
import com.flightpathfinder.rag.service.model.RagStreamEventWriter;

/**
 * 说明。
 *
 * 说明。
 * 说明。
 */
public interface RagStreamService {

    /**
     * 说明。
     *
     * @param command 参数说明。
     * @param eventWriter 参数说明。
     */
    void stream(RagStreamCommand command, RagStreamEventWriter eventWriter);
}
