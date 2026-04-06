package com.flightpathfinder.framework.web;

import com.flightpathfinder.framework.convention.Result;
import com.flightpathfinder.framework.errorcode.BaseErrorCode;
import com.flightpathfinder.framework.exception.AbstractException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
/**
 * 说明。
 */

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AbstractException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(AbstractException ex) {
        return ResponseEntity.badRequest().body(Results.failure(ex));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(Results.failure(BaseErrorCode.CLIENT_INVALID_PARAM_ERROR, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnhandledException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Results.failure(BaseErrorCode.SERVICE_ERROR, ex.getMessage()));
    }
}


