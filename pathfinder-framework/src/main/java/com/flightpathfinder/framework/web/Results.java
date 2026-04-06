package com.flightpathfinder.framework.web;

import com.flightpathfinder.framework.convention.Result;
import com.flightpathfinder.framework.context.RequestIdHolder;
import com.flightpathfinder.framework.errorcode.BaseErrorCode;
import com.flightpathfinder.framework.errorcode.ErrorCode;
import com.flightpathfinder.framework.exception.AbstractException;
import com.flightpathfinder.framework.trace.TraceContext;

/**
 * 统一结果构造工具。
 *
 * 集中封装成功/失败返回模型，并自动附加请求标识用于链路追踪。
 */
public final class Results {

    private Results() {
    }

    /** 构造不含数据体的成功结果。 */
    public static Result<Void> success() {
        return new Result<>(Result.SUCCESS_CODE, null, null, currentRequestId());
    }

    /**
     * 构造带数据的成功结果。
     *
     * @param data 返回数据
     * @return 统一成功结果
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(Result.SUCCESS_CODE, null, data, currentRequestId());
    }

    /**
     * 构造带消息与数据的成功结果。
     *
     * @param message 成功消息
     * @param data 返回数据
     * @return 统一成功结果
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(Result.SUCCESS_CODE, message, data, currentRequestId());
    }

    /** 使用默认服务错误码构造失败结果。 */
    public static Result<Void> failure() {
        return failure(BaseErrorCode.SERVICE_ERROR);
    }

    /**
     * 根据业务异常构造失败结果。
     *
     * @param exception 业务异常
     * @return 统一失败结果
     */
    public static Result<Void> failure(AbstractException exception) {
        return failure(exception.getErrorCode(), exception.getErrorMessage());
    }

    /**
     * 根据错误码构造失败结果。
     *
     * @param errorCode 错误码定义
     * @return 统一失败结果
     */
    public static Result<Void> failure(ErrorCode errorCode) {
        return failure(errorCode.code(), errorCode.message());
    }

    /**
     * 根据错误码与消息构造失败结果。
     *
     * @param errorCode 错误码定义
     * @param message 错误消息
     * @return 统一失败结果
     */
    public static Result<Void> failure(ErrorCode errorCode, String message) {
        return failure(errorCode.code(), message);
    }

    /**
     * 根据原始错误码和消息构造失败结果。
     *
     * @param errorCode 错误码
     * @param message 错误消息
     * @return 统一失败结果
     */
    public static Result<Void> failure(String errorCode, String message) {
        return new Result<>(errorCode, message, null, currentRequestId());
    }

    /**
     * 计算当前请求标识。
     *
        * @return 当前请求标识，缺失时自动生成
     */
    private static String currentRequestId() {
        return RequestIdHolder.current()
                .or(() -> TraceContext.currentRoot().map(root -> root.traceId()))
                .orElseGet(RequestIdHolder::getOrCreate);
    }
}
