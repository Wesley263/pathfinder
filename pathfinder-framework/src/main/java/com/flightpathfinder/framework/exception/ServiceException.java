package com.flightpathfinder.framework.exception;

import com.flightpathfinder.framework.errorcode.ErrorCode;

public class ServiceException extends AbstractException {

    public ServiceException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ServiceException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
