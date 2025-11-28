package com.example.server.codegroup;

import com.example.common.codegroup.annotation.CodeValue;
import com.example.common.codegroup.annotation.ManagedCode;

/**
 * 테스트용 @ManagedCode 어노테이션이 적용된 Enum.
 */
@ManagedCode(
        displayName = "테스트 상태",
        description = "테스트용 상태 코드",
        group = "TEST"
)
public enum TestManagedEnum {

    @CodeValue(label = "대기 중", description = "처리 대기 상태", order = 1)
    PENDING,

    @CodeValue(label = "진행 중", description = "처리 진행 상태", order = 2)
    IN_PROGRESS,

    @CodeValue(label = "완료", description = "처리 완료 상태", order = 3)
    COMPLETED,

    @CodeValue(label = "취소됨", order = 4, deprecated = true)
    CANCELLED
}
