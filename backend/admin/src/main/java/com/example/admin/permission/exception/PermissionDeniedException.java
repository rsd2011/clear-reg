package com.example.admin.permission.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.example.common.error.BusinessException;
import com.example.common.error.CommonErrorCode;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class PermissionDeniedException extends BusinessException {

  public PermissionDeniedException(String message) {
    super(CommonErrorCode.PERMISSION_DENIED, message);
  }
}
