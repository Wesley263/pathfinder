package com.flightpathfinder.infra.ai.chat;
/**
 * 流式聊天回调。
 */
public interface StreamCallback {

    /**
     * 接收流式片段。
     *
     * @param chunk 单个文本片段
     */
    void onChunk(String chunk);

    /**
     * 流式输出完成时回调。
     */
    default void onComplete() {
    }

    /**
     * 流式输出失败时回调。
     *
     * @param throwable 异常对象
     */
    default void onError(Throwable throwable) {
    }
}



