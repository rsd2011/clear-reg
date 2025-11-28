package com.example.server.codegroup;

import com.example.common.codegroup.annotation.ManagedCode;

/**
 * 테스트용 hidden=true 설정된 Enum.
 * 레지스트리에 등록되지 않아야 함.
 */
@ManagedCode(
        displayName = "숨겨진 코드",
        hidden = true
)
public enum TestHiddenEnum {
    HIDDEN_VALUE
}
