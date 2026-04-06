package com.flightpathfinder.framework.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
/**
 * 用于覆盖目标行为的断言并防止回归。
 */
class RequestIdHolderTest {

    @AfterEach
    void tearDown() {
        RequestIdHolder.clear();
    }

    @Test
    void getOrCreate_shouldReuseExistingValue() {
        RequestIdHolder.set("req-fixed");

        String requestId = RequestIdHolder.getOrCreate();

        assertEquals("req-fixed", requestId);
        assertEquals("req-fixed", RequestIdHolder.current().orElse(null));
    }

    @Test
    void getOrCreate_shouldCreateNewValueWhenEmpty() {
        String requestId = RequestIdHolder.getOrCreate();

        assertNotNull(requestId);
        assertFalse(requestId.isBlank());
        assertEquals(requestId, RequestIdHolder.current().orElse(null));
    }

    @Test
    void setBlank_shouldClearHolder() {
        RequestIdHolder.set("req-1");
        RequestIdHolder.set(" ");

        assertTrue(RequestIdHolder.current().isEmpty());
    }

    @Test
    void values_shouldBeIsolatedAcrossThreads() throws InterruptedException {
        RequestIdHolder.set("main-thread-id");
        AtomicReference<String> workerValue = new AtomicReference<>();

        Thread worker = new Thread(() -> workerValue.set(RequestIdHolder.current().orElse(null)));
        worker.start();
        worker.join();

        assertEquals("main-thread-id", RequestIdHolder.current().orElse(null));
        assertNull(workerValue.get());
    }
}



