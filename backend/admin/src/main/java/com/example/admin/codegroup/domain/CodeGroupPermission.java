package com.example.admin.codegroup.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 코드 그룹 권한.
 *
 * <p>CodeGroupSource가 가질 수 있는 권한을 정의합니다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum CodeGroupPermission {

    /** 기존 아이템 수정(오버라이드) 가능 */
    EDITABLE("수정 가능"),

    /** 새 아이템 생성 가능 */
    CREATABLE("생성 가능"),

    /** 아이템 삭제 가능 */
    DELETABLE("삭제 가능"),

    /** DB 저장 필요 */
    REQUIRES_DB_STORAGE("DB 저장 필요");

    private final String description;
}
