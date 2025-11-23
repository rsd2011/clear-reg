package com.example.dw.application.readmodel;

/**
 * 사용자별 Permission Menu Read Model 생성 소스.
 */
public interface PermissionMenuReadModelSource {

    PermissionMenuReadModel snapshot(String principalId);
}
