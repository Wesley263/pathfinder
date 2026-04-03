package com.flightpathfinder.infra.ai.chat;

public interface StreamCallback {

    void onChunk(String chunk);

    default void onComplete() {
    }

    default void onError(Throwable throwable) {
    }
}

