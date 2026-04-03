package com.flightpathfinder.framework.convention;

import java.io.Serial;
import java.io.Serializable;

public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 372495392555590341L;

    public static final String SUCCESS_CODE = "0";

    private final String code;
    private final String message;
    private final T data;
    private final String requestId;

    public Result(String code, String message, T data, String requestId) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.requestId = requestId;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public String getRequestId() {
        return requestId;
    }

    public boolean isSuccess() {
        return SUCCESS_CODE.equals(code);
    }
}
