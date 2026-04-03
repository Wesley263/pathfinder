package com.flightpathfinder.framework.exception;

import com.flightpathfinder.framework.errorcode.ErrorCode;
import java.util.Optional;

public abstract class AbstractException extends RuntimeException {

    private final String errorCode;
    private final String errorMessage;

    protected AbstractException(ErrorCode errorCode) {
        this(null, null, errorCode);
    }

    protected AbstractException(ErrorCode errorCode, String message) {
        this(message, null, errorCode);
    }

    protected AbstractException(String message, Throwable throwable, ErrorCode errorCode) {
        super(message, throwable);
        this.errorCode = errorCode.code();
        this.errorMessage = Optional.ofNullable(message).filter(value -> !value.isBlank()).orElse(errorCode.message());
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
