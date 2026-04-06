package com.flightpathfinder.framework.exception;

import com.flightpathfinder.framework.errorcode.ErrorCode;
import java.util.Optional;

/**
 * 业务异常基类。
 *
 * 统一封装错误码与对外错误消息，便于全局异常处理组件标准化输出。
 */
public abstract class AbstractException extends RuntimeException {

    /** 标准化错误码。 */
    private final String errorCode;
    /** 标准化错误消息。 */
    private final String errorMessage;

    /**
     * 使用错误码构造异常。
     *
     * @param errorCode 错误码定义
     */
    protected AbstractException(ErrorCode errorCode) {
        this(null, null, errorCode);
    }

    /**
     * 使用错误码与消息构造异常。
     *
     * @param errorCode 错误码定义
     * @param message 自定义错误消息
     */
    protected AbstractException(ErrorCode errorCode, String message) {
        this(message, null, errorCode);
    }

    /**
     * 使用完整参数构造异常。
     *
     * @param message 错误消息
     * @param throwable 根因异常
     * @param errorCode 错误码定义
     */
    protected AbstractException(String message, Throwable throwable, ErrorCode errorCode) {
        super(message, throwable);
        this.errorCode = errorCode.code();
        this.errorMessage = Optional.ofNullable(message).filter(value -> !value.isBlank()).orElse(errorCode.message());
    }

    /** 返回标准化错误码。 */
    public String getErrorCode() {
        return errorCode;
    }

    /** 返回标准化错误消息。 */
    public String getErrorMessage() {
        return errorMessage;
    }
}
