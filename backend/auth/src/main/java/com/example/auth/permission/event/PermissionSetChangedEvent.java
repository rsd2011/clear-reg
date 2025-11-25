package com.example.auth.permission.event;

import org.springframework.lang.Nullable;

/** 권한/RowScope 변경 시 발생시키는 이벤트. principalId 가 null이면 전체 권한 변경으로 간주. */
public record PermissionSetChangedEvent(@Nullable String principalId) {}
