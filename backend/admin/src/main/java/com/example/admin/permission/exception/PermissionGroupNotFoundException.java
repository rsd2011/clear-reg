package com.example.admin.permission.exception;

/**
 * 권한 그룹을 찾을 수 없을 때 발생하는 예외.
 */
public class PermissionGroupNotFoundException extends RuntimeException {

    public PermissionGroupNotFoundException(String message) {
        super(message);
    }
}
