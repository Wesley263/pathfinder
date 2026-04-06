package com.flightpathfinder.framework.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.flightpathfinder.framework.errorcode.BaseErrorCode;
import org.junit.jupiter.api.Test;
/**
 * 测试用例。
 */
class ServiceExceptionTest {

    @Test
    void constructor_shouldUseErrorCodeMessageWhenCustomMessageMissing() {
        ServiceException exception = new ServiceException(BaseErrorCode.SERVICE_ERROR);

        assertEquals(BaseErrorCode.SERVICE_ERROR.code(), exception.getErrorCode());
        assertEquals(BaseErrorCode.SERVICE_ERROR.message(), exception.getErrorMessage());
    }

    @Test
    void constructor_shouldPreferCustomMessage() {
        ServiceException exception = new ServiceException(BaseErrorCode.CLIENT_ERROR, "custom message");

        assertEquals(BaseErrorCode.CLIENT_ERROR.code(), exception.getErrorCode());
        assertEquals("custom message", exception.getErrorMessage());
    }
}

