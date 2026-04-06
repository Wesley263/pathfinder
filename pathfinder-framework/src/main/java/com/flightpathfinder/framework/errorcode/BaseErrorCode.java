package com.flightpathfinder.framework.errorcode;
/**
 * 错误码体系基础类型。
 */
public enum BaseErrorCode implements ErrorCode {
    CLIENT_ERROR("A000001", "Client error"),
    CLIENT_INVALID_PARAM_ERROR("A000300", "Invalid request parameter"),
    SERVICE_ERROR("B000001", "Service execution error"),
    REMOTE_ERROR("C000001", "Remote service error");

    private final String code;
    private final String message;

    BaseErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}


