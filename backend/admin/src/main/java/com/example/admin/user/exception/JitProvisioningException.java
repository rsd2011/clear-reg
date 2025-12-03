package com.example.admin.user.exception;

/**
 * JIT Provisioning 처리 중 발생하는 예외.
 */
public class JitProvisioningException extends RuntimeException {

  public JitProvisioningException(String message) {
    super(message);
  }

  public JitProvisioningException(String message, Throwable cause) {
    super(message, cause);
  }
}
