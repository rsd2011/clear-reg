package com.example.auth;

import com.example.common.error.BusinessException;
import com.example.common.error.CommonErrorCode;

public class InvalidCredentialsException extends BusinessException {

  public InvalidCredentialsException() {
    super(CommonErrorCode.PERMISSION_DENIED, "Invalid username or password");
  }
}
