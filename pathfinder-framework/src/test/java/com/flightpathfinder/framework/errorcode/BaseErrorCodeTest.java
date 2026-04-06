package com.flightpathfinder.framework.errorcode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
/**
 * 用于覆盖目标行为的断言并防止回归。
 */
class BaseErrorCodeTest {

    @Test
    void values_shouldExposeUniqueNonBlankCodeAndMessage() {
        BaseErrorCode[] values = BaseErrorCode.values();
        Set<String> codes = Arrays.stream(values).map(BaseErrorCode::code).collect(Collectors.toSet());

        assertEquals(values.length, codes.size());
        assertTrue(Arrays.stream(values).noneMatch(value -> value.code() == null || value.code().isBlank()));
        assertTrue(Arrays.stream(values).noneMatch(value -> value.message() == null || value.message().isBlank()));
    }

    @Test
    void serviceError_shouldKeepExpectedConstantValue() {
        assertEquals("B000001", BaseErrorCode.SERVICE_ERROR.code());
        assertFalse(BaseErrorCode.SERVICE_ERROR.message().isBlank());
    }
}



