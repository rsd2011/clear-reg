package com.example.admin.systemconfig.exception;

/**
 * 시스템 설정을 찾을 수 없을 때 발생하는 예외.
 */
public class SystemConfigNotFoundException extends RuntimeException {

  public SystemConfigNotFoundException(String message) {
    super(message);
  }

  public SystemConfigNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
