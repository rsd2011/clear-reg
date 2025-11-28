package com.example.admin.codegroup.domain;

import static com.example.admin.codegroup.domain.CodeGroupPermission.*;

import java.util.EnumSet;
import java.util.Set;

import lombok.Getter;

/**
 * 코드 그룹 소스 타입.
 *
 * <p>우선순위: DB 설정 > 어노테이션 > Enum 기본값</p>
 *
 * <p>권한 매트릭스:</p>
 * <table>
 *   <tr><th>Source</th><th>editable</th><th>creatable</th><th>deletable</th><th>삭제 동작</th><th>설명</th></tr>
 *   <tr><td>STATIC_ENUM</td><td>✓</td><td>✗</td><td>✓</td><td>영구 삭제</td><td>오버라이드 가능, 삭제 시 데이터 삭제</td></tr>
 *   <tr><td>DYNAMIC_DB</td><td>✓</td><td>✓</td><td>✓</td><td>영구 삭제</td><td>CRUD 모두 가능</td></tr>
 *   <tr><td>LOCALE_COUNTRY</td><td>✓</td><td>✓</td><td>✓</td><td>원복</td><td>ISO 국가 코드, 삭제 시 ISO 원본 복원</td></tr>
 *   <tr><td>LOCALE_LANGUAGE</td><td>✓</td><td>✓</td><td>✓</td><td>원복</td><td>ISO 언어 코드, 삭제 시 ISO 원본 복원</td></tr>
 *   <tr><td>DW</td><td>✗</td><td>✗</td><td>✗</td><td>-</td><td>외부 연동 (읽기 전용)</td></tr>
 *   <tr><td>APPROVAL_GROUP</td><td>✗</td><td>✗</td><td>✗</td><td>-</td><td>Entity 기반 (읽기 전용)</td></tr>
 * </table>
 */
@Getter
public enum CodeGroupSource {

    // === 정적 소스 (오버라이드 가능) ===
    /**
     * 프로젝트 내 Enum 기반 정적 코드.
     * <ul>
     *   <li>editable: 기존 Enum 값의 라벨/순서 등 DB 오버라이드 가능</li>
     *   <li>creatable: 새로운 코드 생성 불가 (Enum에 정의된 값만 사용)</li>
     *   <li>deletable: DB 오버라이드 데이터 삭제 가능 (영구 삭제)</li>
     * </ul>
     */
    STATIC_ENUM(EnumSet.of(EDITABLE, DELETABLE, REQUIRES_DB_STORAGE)),

    // === 동적 소스 (CRUD 가능) ===
    /**
     * DB에서 관리되는 동적 공통코드.
     * 관리자가 생성/수정/삭제 모두 가능.
     */
    DYNAMIC_DB(EnumSet.of(EDITABLE, CREATABLE, DELETABLE, REQUIRES_DB_STORAGE)),

    // === Locale 기반 소스 (ISO 표준 + 커스텀) ===
    /**
     * ISO 3166-1 국가 코드.
     * <ul>
     *   <li>editable: ISO 항목의 한글명 오버라이드 가능</li>
     *   <li>creatable: 커스텀 국가 추가 가능</li>
     *   <li>deletable: 삭제 시 ISO 원본으로 복원 (builtIn=true), 커스텀은 영구 삭제 (builtIn=false)</li>
     * </ul>
     */
    LOCALE_COUNTRY(EnumSet.of(EDITABLE, CREATABLE, DELETABLE, REQUIRES_DB_STORAGE)),

    /**
     * ISO 639 언어 코드.
     * <ul>
     *   <li>editable: ISO 항목의 한글명 오버라이드 가능</li>
     *   <li>creatable: 커스텀 언어 추가 가능</li>
     *   <li>deletable: 삭제 시 ISO 원본으로 복원 (builtIn=true), 커스텀은 영구 삭제 (builtIn=false)</li>
     * </ul>
     */
    LOCALE_LANGUAGE(EnumSet.of(EDITABLE, CREATABLE, DELETABLE, REQUIRES_DB_STORAGE)),

    // === 외부 연동 소스 (읽기 전용) ===
    /**
     * 데이터웨어하우스 연동 코드.
     * 외부 시스템에서 동기화되며 수정 불가.
     */
    DW(EnumSet.noneOf(CodeGroupPermission.class)),

    /**
     * 승인 그룹 Entity 기반 코드.
     * ApprovalGroup Entity에서 자동 생성되며 수정 불가.
     */
    APPROVAL_GROUP(EnumSet.noneOf(CodeGroupPermission.class));

    private final Set<CodeGroupPermission> permissions;

    CodeGroupSource(EnumSet<CodeGroupPermission> permissions) {
        this.permissions = permissions;
    }

    /**
     * 특정 권한을 가지고 있는지 확인
     */
    public boolean hasPermission(CodeGroupPermission permission) {
        return permissions.contains(permission);
    }

    /**
     * 기존 아이템 수정(오버라이드) 가능 여부
     */
    public boolean isEditable() {
        return hasPermission(EDITABLE);
    }

    /**
     * 새 아이템 생성 가능 여부
     */
    public boolean isCreatable() {
        return hasPermission(CREATABLE);
    }

    /**
     * 아이템 삭제 가능 여부
     */
    public boolean isDeletable() {
        return hasPermission(DELETABLE);
    }

    /**
     * DB 저장 필요 여부
     */
    public boolean isRequiresDbStorage() {
        return hasPermission(REQUIRES_DB_STORAGE);
    }

    /**
     * 읽기 전용 여부 (editable, creatable, deletable 모두 없으면 읽기 전용)
     */
    public boolean isReadOnly() {
        return !isEditable() && !isCreatable() && !isDeletable();
    }

    /**
     * 정적 Enum 기반 소스인지 확인
     */
    public boolean isStaticEnum() {
        return this == STATIC_ENUM;
    }

    /**
     * 외부 소스인지 확인
     */
    public boolean isExternal() {
        return this == DW;
    }

    /**
     * 동적 DB 소스인지 확인
     */
    public boolean isDynamic() {
        return this == DYNAMIC_DB;
    }

    /**
     * Locale 기반 소스인지 확인 (국가/언어 코드)
     */
    public boolean isLocale() {
        return this == LOCALE_COUNTRY || this == LOCALE_LANGUAGE;
    }

    /**
     * 삭제 시 원복되는 소스인지 확인.
     * <p>LOCALE_COUNTRY, LOCALE_LANGUAGE는 삭제 시 ISO 원본으로 복원됩니다.</p>
     */
    public boolean isRestoreOnDelete() {
        return isLocale();
    }
}
