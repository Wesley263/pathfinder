package com.flightpathfinder.rag.service.model;

/**
 * 流式事件输出器。
 *
 * <p>它把流式服务与具体输出介质解耦，当前 controller 使用 SSE 实现，后续也可以替换为别的事件通道。</p>
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
