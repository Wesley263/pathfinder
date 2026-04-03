package com.flightpathfinder.rag.service.model;

public interface RagStreamEventWriter {

    void emit(RagStreamEvent event);

    default void complete() {
    }
}
