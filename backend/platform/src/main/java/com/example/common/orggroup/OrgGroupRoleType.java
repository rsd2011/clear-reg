package com.example.common.orggroup;

import com.example.common.codegroup.annotation.ManagedCode;

/**
 * 조직그룹 역할 유형.
 *
 * <p>조직그룹 내에서 사용자의 역할에 따라 다른 권한그룹을 적용할 수 있다.</p>
 */
@ManagedCode
public enum OrgGroupRoleType {

    /** 리더 역할 */
    LEADER,

    /** 매니저 역할 */
    MANAGER,

    /** 일반 구성원 역할 */
    MEMBER;

    /**
     * 문자열로부터 OrgGroupRoleType을 반환한다.
     *
     * @param code 역할 코드
     * @return 해당 OrgGroupRoleType, 없으면 null
     */
    public static OrgGroupRoleType fromString(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
