package com.flightpathfinder.framework.web;

import com.flightpathfinder.framework.convention.Result;
import com.flightpathfinder.framework.context.RequestIdHolder;
import com.flightpathfinder.framework.errorcode.BaseErrorCode;
import com.flightpathfinder.framework.errorcode.ErrorCode;
import com.flightpathfinder.framework.exception.AbstractException;
import com.flightpathfinder.framework.trace.TraceContext;

public final class Results {

    private Results() {
    }

    public static Result<Void> success() {
        return new Result<>(Result.SUCCESS_CODE, null, null, currentRequestId());
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(Result.SUCCESS_CODE, null, data, currentRequestId());
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(Result.SUCCESS_CODE, message, data, currentRequestId());
    }

    public static Result<Void> failure() {
        return failure(BaseErrorCode.SERVICE_ERROR);
    }

    public static Result<Void> failure(AbstractException exception) {
        return failure(exception.getErrorCode(), exception.getErrorMessage());
    }

    public static Result<Void> failure(ErrorCode errorCode) {
        return failure(errorCode.code(), errorCode.message());
    }

    public static Result<Void> failure(ErrorCode errorCode, String message) {
        return failure(errorCode.code(), message);
    }

    public static Result<Void> failure(String errorCode, String message) {
        return new Result<>(errorCode, message, null, currentRequestId());
    }

    private static String currentRequestId() {
        return RequestIdHolder.current()
                .or(() -> TraceContext.currentRoot().map(root -> root.traceId()))
                .orElseGet(RequestIdHolder::getOrCreate);
    }
}
