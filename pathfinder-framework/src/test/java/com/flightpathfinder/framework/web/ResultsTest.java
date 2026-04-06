package com.flightpathfinder.framework.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flightpathfinder.framework.convention.Result;
import com.flightpathfinder.framework.context.RequestIdHolder;
import com.flightpathfinder.framework.errorcode.BaseErrorCode;
import com.flightpathfinder.framework.exception.ServiceException;
import com.flightpathfinder.framework.trace.TraceContext;
import com.flightpathfinder.framework.trace.TraceRoot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
/**
 * 用于覆盖目标行为的断言并防止回归。
 */
class ResultsTest {

    @AfterEach
    void tearDown() {
        RequestIdHolder.clear();
        TraceContext.clear();
    }

    @Test
    void success_shouldUseRequestIdFromRequestIdHolderFirst() {
        RequestIdHolder.set("req-direct");

        Result<String> result = Results.success("payload");

        assertTrue(result.isSuccess());
        assertEquals("req-direct", result.getRequestId());
        assertEquals("payload", result.getData());
    }

    @Test
    void success_shouldFallbackToTraceIdWhenRequestIdMissing() {
        TraceRoot root = TraceContext.startRoot("scene-1");

        Result<Void> result = Results.success();

        assertEquals(root.traceId(), result.getRequestId());
        assertTrue(result.isSuccess());
    }

    @Test
    void failure_shouldCreateRequestIdWhenNoContextAvailable() {
        Result<Void> result = Results.failure(BaseErrorCode.CLIENT_ERROR);

        assertFalse(result.isSuccess());
        assertEquals(BaseErrorCode.CLIENT_ERROR.code(), result.getCode());
        assertFalse(result.getRequestId().isBlank());
        assertEquals(result.getRequestId(), RequestIdHolder.current().orElse(null));
    }

    @Test
    void failure_shouldUseExceptionCodeAndMessage() {
        RequestIdHolder.set("req-ex");
        ServiceException exception = new ServiceException(BaseErrorCode.REMOTE_ERROR, "remote timeout");

        Result<Void> result = Results.failure(exception);

        assertEquals("req-ex", result.getRequestId());
        assertEquals(BaseErrorCode.REMOTE_ERROR.code(), result.getCode());
        assertEquals("remote timeout", result.getMessage());
    }
}



