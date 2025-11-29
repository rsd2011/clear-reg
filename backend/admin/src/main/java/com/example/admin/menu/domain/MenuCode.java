package com.example.admin.menu.domain;

import com.example.common.codegroup.annotation.ManagedCode;

/**
 * 시스템 메뉴 코드.
 *
 * <p>프론트엔드 라우팅 경로와 1:1 매핑되며, 개발자가 정의한다.</p>
 * <p>path는 프론트엔드 라우터에서 사용되는 경로이다.</p>
 * <p>defaultIcon은 아이콘 라이브러리(예: Lucide, FontAwesome)의 아이콘 이름이다.</p>
 */
@ManagedCode(displayName = "메뉴 코드", group = "SYSTEM")
public enum MenuCode {

    // ========== 대시보드 ==========
    DASHBOARD("/dashboard", "home"),

    // ========== 기안/결재 ==========
    DRAFT("/drafts", "edit"),
    DRAFT_CREATE("/drafts/new", "plus"),
    APPROVAL("/approvals", "check-square"),
    APPROVAL_INBOX("/approvals/inbox", "inbox"),
    APPROVAL_SENT("/approvals/sent", "send"),

    // ========== 관리 ==========
    USER_MGMT("/admin/users", "users"),
    PERMISSION_MGMT("/admin/permissions", "shield"),
    MENU_MGMT("/admin/menus", "menu"),
    ORG_MGMT("/admin/organizations", "building"),

    // ========== 모니터링 ==========
    AUDIT_LOG("/audit/logs", "file-text"),
    AUDIT_REPORT("/audit/reports", "bar-chart"),

    // ========== 시스템 ==========
    COMMON_CODE("/system/codes", "list"),
    NOTICE("/system/notices", "bell"),
    ;

    private final String path;
    private final String defaultIcon;

    MenuCode(String path, String defaultIcon) {
        this.path = path;
        this.defaultIcon = defaultIcon;
    }

    /**
     * 프론트엔드 라우팅 경로.
     */
    public String getPath() {
        return path;
    }

    /**
     * 기본 아이콘 이름.
     */
    public String getDefaultIcon() {
        return defaultIcon;
    }

    /**
     * 경로로 MenuCode 조회.
     *
     * @param path 프론트엔드 경로
     * @return 매칭되는 MenuCode, 없으면 null
     */
    public static MenuCode fromPath(String path) {
        if (path == null) {
            return null;
        }
        for (MenuCode code : values()) {
            if (code.path.equals(path)) {
                return code;
            }
        }
        return null;
    }

    /**
     * 문자열로 MenuCode 조회 (대소문자 무시).
     *
     * @param code 코드 문자열
     * @return 매칭되는 MenuCode, 없으면 null
     */
    public static MenuCode fromString(String code) {
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
