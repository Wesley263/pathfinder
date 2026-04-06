package com.flightpathfinder.infra.ai.chat;
/**
 * 面向 AI 聊天的能力接口定义。
 */
public interface StreamCallback {

    void onChunk(String chunk);

    default void onComplete() {
    }

    default void onError(Throwable throwable) {
    }
}



