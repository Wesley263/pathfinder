package com.flightpathfinder.rag.service.model;

/**
 * 流式事件输出器。
 *
 * 说明。
 */
public interface RagStreamEventWriter {

    /**
     * 输出一条流式事件。
     *
     * @param event 待发送事件
     */
    void emit(RagStreamEvent event);

    /**
     * 通知事件流结束。
     */
    default void complete() {
    }
}
