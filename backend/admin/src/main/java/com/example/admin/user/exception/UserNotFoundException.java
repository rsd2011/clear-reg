package com.example.admin.user.exception;

/**
 * 사용자를 찾을 수 없을 때 발생하는 예외.
 */
public class UserNotFoundException extends RuntimeException {

  private final String username;

  public UserNotFoundException(String username) {
    super("사용자를 찾을 수 없습니다: " + username);
    this.username = username;
  }

  public String getUsername() {
    return username;
  }
}
