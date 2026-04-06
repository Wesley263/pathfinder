package com.flightpathfinder.rag.service.model;

/**
 * 流式事件输出器。
 *
 * 用于定义当前类型或方法在模块内的职责边界。
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


