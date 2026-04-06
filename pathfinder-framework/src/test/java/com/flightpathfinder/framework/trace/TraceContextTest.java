package com.flightpathfinder.framework.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
/**
 * 测试用例。
 */
class TraceContextTest {

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    @Test
    void startRoot_shouldBindRootIntoCurrentThread() {
        TraceRoot root = TraceContext.startRoot("rag-chat");

        assertFalse(root.traceId().isBlank());
        assertEquals("rag-chat", root.scene());
        assertTrue(TraceContext.currentRoot().isPresent());
        assertEquals(root.traceId(), TraceContext.currentRoot().orElseThrow().traceId());
    }

    @Test
    void clear_shouldRemoveCurrentRoot() {
        TraceContext.startRoot("scene");
        TraceContext.clear();

        assertTrue(TraceContext.currentRoot().isEmpty());
    }
}

