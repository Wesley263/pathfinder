package com.flightpathfinder.infra.ai.chat;
/**
 * 说明。
 */
public interface StreamCallback {

    void onChunk(String chunk);

    default void onComplete() {
    }

    default void onError(Throwable throwable) {
    }
}



