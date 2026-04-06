package com.flightpathfinder.framework.convention;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
/**
 * 测试用例。
 */
class ResultTest {

    @Test
    void isSuccess_shouldReturnTrueWhenCodeIsZero() {
        Result<String> result = new Result<>(Result.SUCCESS_CODE, "ok", "payload", "req-1");

        assertTrue(result.isSuccess());
        assertEquals("payload", result.getData());
        assertEquals("req-1", result.getRequestId());
    }

    @Test
    void isSuccess_shouldReturnFalseWhenCodeIsNotZero() {
        Result<Void> result = new Result<>("B000001", "failed", null, "req-2");

        assertFalse(result.isSuccess());
    }
}

