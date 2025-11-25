package com.example.dwworker.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.common.api.ErrorResponse;
import com.example.common.error.BusinessException;
import com.example.common.error.CommonErrorCode;
import com.example.common.error.ErrorCode;

@RestControllerAdvice(basePackages = "com.example.dwworker")
public class DwWorkerExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException exception) {
        ErrorCode code = exception.errorCode();
        return ResponseEntity.status(toStatus(code))
                .body(new ErrorResponse(code != null ? code.code() : CommonErrorCode.INTERNAL_ERROR.code(),
                        exception.getMessage()));
    }

    private HttpStatus toStatus(ErrorCode code) {
        if (code == null) return HttpStatus.INTERNAL_SERVER_ERROR;
        if (code == CommonErrorCode.INVALID_REQUEST) return HttpStatus.BAD_REQUEST;
        if (code == CommonErrorCode.PERMISSION_DENIED) return HttpStatus.FORBIDDEN;
        if (code == CommonErrorCode.NOT_FOUND) return HttpStatus.NOT_FOUND;
        if (code == CommonErrorCode.CONFLICT) return HttpStatus.CONFLICT;
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
