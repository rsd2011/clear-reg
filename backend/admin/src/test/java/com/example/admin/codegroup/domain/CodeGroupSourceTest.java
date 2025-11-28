package com.example.admin.codegroup.domain;

import static com.example.admin.codegroup.domain.CodeGroupPermission.*;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CodeGroupSourceTest {

    @Test
    @DisplayName("STATIC_ENUM은 editable=true (기존 Enum 값의 라벨/순서 등 오버라이드 가능)")
    void staticEnumIsEditable() {
        assertThat(CodeGroupSource.STATIC_ENUM.isEditable()).isTrue();
    }

    @Test
    @DisplayName("DYNAMIC_DB는 수정 가능해야 한다")
    void dynamicDbIsEditable() {
        assertThat(CodeGroupSource.DYNAMIC_DB.isEditable()).isTrue();
    }

    @Test
    @DisplayName("DW는 수정 불가능해야 한다")
    void dwIsNotEditable() {
        assertThat(CodeGroupSource.DW.isEditable()).isFalse();
    }

    @Test
    @DisplayName("APPROVAL_GROUP은 수정 불가능해야 한다")
    void approvalGroupIsNotEditable() {
        assertThat(CodeGroupSource.APPROVAL_GROUP.isEditable()).isFalse();
    }

    @Test
    @DisplayName("STATIC_ENUM은 읽기 전용이 아니다 (오버라이드 수정 가능)")
    void staticEnumIsNotReadOnly() {
        // STATIC_ENUM은 EDITABLE 권한이 있으므로 읽기 전용이 아님
        assertThat(CodeGroupSource.STATIC_ENUM.isReadOnly()).isFalse();
    }

    @Test
    @DisplayName("DYNAMIC_DB는 읽기 전용이 아니어야 한다")
    void dynamicDbIsNotReadOnly() {
        assertThat(CodeGroupSource.DYNAMIC_DB.isReadOnly()).isFalse();
    }

    @Test
    @DisplayName("DW는 읽기 전용이어야 한다")
    void dwIsReadOnly() {
        assertThat(CodeGroupSource.DW.isReadOnly()).isTrue();
    }

    @Test
    @DisplayName("APPROVAL_GROUP은 읽기 전용이어야 한다")
    void approvalGroupIsReadOnly() {
        assertThat(CodeGroupSource.APPROVAL_GROUP.isReadOnly()).isTrue();
    }

    @Test
    @DisplayName("DYNAMIC_DB는 DB 저장이 필요하다")
    void dynamicDbRequiresDbStorage() {
        assertThat(CodeGroupSource.DYNAMIC_DB.isRequiresDbStorage()).isTrue();
    }

    @Test
    @DisplayName("STATIC_ENUM은 DB 저장이 필요하다 (오버라이드 데이터 저장)")
    void staticEnumRequiresDbStorage() {
        assertThat(CodeGroupSource.STATIC_ENUM.isRequiresDbStorage()).isTrue();
    }

    @Test
    @DisplayName("STATIC_ENUM은 정적 Enum 소스이다")
    void staticEnumIsStaticEnum() {
        assertThat(CodeGroupSource.STATIC_ENUM.isStaticEnum()).isTrue();
    }

    @Test
    @DisplayName("DYNAMIC_DB는 정적 Enum 소스가 아니다")
    void dynamicDbIsNotStaticEnum() {
        assertThat(CodeGroupSource.DYNAMIC_DB.isStaticEnum()).isFalse();
    }

    @Test
    @DisplayName("DW는 외부 소스이다")
    void dwIsExternal() {
        assertThat(CodeGroupSource.DW.isExternal()).isTrue();
    }

    @Test
    @DisplayName("DYNAMIC_DB는 외부 소스가 아니다")
    void dynamicDbIsNotExternal() {
        assertThat(CodeGroupSource.DYNAMIC_DB.isExternal()).isFalse();
    }

    @Test
    @DisplayName("DYNAMIC_DB는 동적 소스이다")
    void dynamicDbIsDynamic() {
        assertThat(CodeGroupSource.DYNAMIC_DB.isDynamic()).isTrue();
    }

    @Test
    @DisplayName("STATIC_ENUM은 동적 소스가 아니다")
    void staticEnumIsNotDynamic() {
        assertThat(CodeGroupSource.STATIC_ENUM.isDynamic()).isFalse();
    }

    // ========== isCreatable 테스트 ==========

    @Test
    @DisplayName("DYNAMIC_DB, LOCALE_COUNTRY, LOCALE_LANGUAGE는 새 아이템 생성이 가능하다")
    void creatableSources() {
        assertThat(CodeGroupSource.DYNAMIC_DB.isCreatable()).isTrue();
        assertThat(CodeGroupSource.LOCALE_COUNTRY.isCreatable()).isTrue();
        assertThat(CodeGroupSource.LOCALE_LANGUAGE.isCreatable()).isTrue();
        assertThat(CodeGroupSource.STATIC_ENUM.isCreatable()).isFalse();
        assertThat(CodeGroupSource.DW.isCreatable()).isFalse();
        assertThat(CodeGroupSource.APPROVAL_GROUP.isCreatable()).isFalse();
    }

    // ========== isDeletable 테스트 ==========

    @Test
    @DisplayName("DW, APPROVAL_GROUP을 제외한 모든 소스는 삭제가 가능하다")
    void deletableSources() {
        assertThat(CodeGroupSource.DYNAMIC_DB.isDeletable()).isTrue();
        assertThat(CodeGroupSource.STATIC_ENUM.isDeletable()).isTrue();
        assertThat(CodeGroupSource.LOCALE_COUNTRY.isDeletable()).isTrue();
        assertThat(CodeGroupSource.LOCALE_LANGUAGE.isDeletable()).isTrue();
        assertThat(CodeGroupSource.DW.isDeletable()).isFalse();
        assertThat(CodeGroupSource.APPROVAL_GROUP.isDeletable()).isFalse();
    }

    // ========== hasPermission 테스트 ==========

    @Test
    @DisplayName("STATIC_ENUM은 EDITABLE, DELETABLE, REQUIRES_DB_STORAGE 권한을 가진다")
    void staticEnumPermissions() {
        assertThat(CodeGroupSource.STATIC_ENUM.hasPermission(EDITABLE)).isTrue();
        assertThat(CodeGroupSource.STATIC_ENUM.hasPermission(CREATABLE)).isFalse();
        assertThat(CodeGroupSource.STATIC_ENUM.hasPermission(DELETABLE)).isTrue();
        assertThat(CodeGroupSource.STATIC_ENUM.hasPermission(REQUIRES_DB_STORAGE)).isTrue();
    }

    @Test
    @DisplayName("DYNAMIC_DB는 모든 권한을 가진다")
    void dynamicDbHasAllPermissions() {
        assertThat(CodeGroupSource.DYNAMIC_DB.hasPermission(EDITABLE)).isTrue();
        assertThat(CodeGroupSource.DYNAMIC_DB.hasPermission(CREATABLE)).isTrue();
        assertThat(CodeGroupSource.DYNAMIC_DB.hasPermission(DELETABLE)).isTrue();
        assertThat(CodeGroupSource.DYNAMIC_DB.hasPermission(REQUIRES_DB_STORAGE)).isTrue();
    }

    @Test
    @DisplayName("LOCALE_COUNTRY는 모든 권한을 가진다")
    void localeCountryHasAllPermissions() {
        assertThat(CodeGroupSource.LOCALE_COUNTRY.hasPermission(EDITABLE)).isTrue();
        assertThat(CodeGroupSource.LOCALE_COUNTRY.hasPermission(CREATABLE)).isTrue();
        assertThat(CodeGroupSource.LOCALE_COUNTRY.hasPermission(DELETABLE)).isTrue();
        assertThat(CodeGroupSource.LOCALE_COUNTRY.hasPermission(REQUIRES_DB_STORAGE)).isTrue();
    }

    @Test
    @DisplayName("LOCALE_LANGUAGE는 모든 권한을 가진다")
    void localeLanguageHasAllPermissions() {
        assertThat(CodeGroupSource.LOCALE_LANGUAGE.hasPermission(EDITABLE)).isTrue();
        assertThat(CodeGroupSource.LOCALE_LANGUAGE.hasPermission(CREATABLE)).isTrue();
        assertThat(CodeGroupSource.LOCALE_LANGUAGE.hasPermission(DELETABLE)).isTrue();
        assertThat(CodeGroupSource.LOCALE_LANGUAGE.hasPermission(REQUIRES_DB_STORAGE)).isTrue();
    }

    @Test
    @DisplayName("DW는 아무 권한도 없다")
    void dwHasNoPermissions() {
        assertThat(CodeGroupSource.DW.hasPermission(EDITABLE)).isFalse();
        assertThat(CodeGroupSource.DW.hasPermission(CREATABLE)).isFalse();
        assertThat(CodeGroupSource.DW.hasPermission(DELETABLE)).isFalse();
        assertThat(CodeGroupSource.DW.hasPermission(REQUIRES_DB_STORAGE)).isFalse();
    }

    @Test
    @DisplayName("APPROVAL_GROUP은 아무 권한도 없다")
    void approvalGroupHasNoPermissions() {
        assertThat(CodeGroupSource.APPROVAL_GROUP.hasPermission(EDITABLE)).isFalse();
        assertThat(CodeGroupSource.APPROVAL_GROUP.hasPermission(CREATABLE)).isFalse();
        assertThat(CodeGroupSource.APPROVAL_GROUP.hasPermission(DELETABLE)).isFalse();
        assertThat(CodeGroupSource.APPROVAL_GROUP.hasPermission(REQUIRES_DB_STORAGE)).isFalse();
    }

    @Test
    @DisplayName("getPermissions()로 권한 Set을 조회할 수 있다")
    void getPermissionsReturnsPermissionSet() {
        assertThat(CodeGroupSource.DYNAMIC_DB.getPermissions())
                .containsExactlyInAnyOrder(EDITABLE, CREATABLE, DELETABLE, REQUIRES_DB_STORAGE);
        assertThat(CodeGroupSource.STATIC_ENUM.getPermissions())
                .containsExactlyInAnyOrder(EDITABLE, DELETABLE, REQUIRES_DB_STORAGE);
        assertThat(CodeGroupSource.LOCALE_COUNTRY.getPermissions())
                .containsExactlyInAnyOrder(EDITABLE, CREATABLE, DELETABLE, REQUIRES_DB_STORAGE);
        assertThat(CodeGroupSource.LOCALE_LANGUAGE.getPermissions())
                .containsExactlyInAnyOrder(EDITABLE, CREATABLE, DELETABLE, REQUIRES_DB_STORAGE);
        assertThat(CodeGroupSource.DW.getPermissions()).isEmpty();
        assertThat(CodeGroupSource.APPROVAL_GROUP.getPermissions()).isEmpty();
    }

    // ========== isLocale 테스트 ==========

    @Test
    @DisplayName("LOCALE_COUNTRY와 LOCALE_LANGUAGE는 Locale 소스이다")
    void localeSources() {
        assertThat(CodeGroupSource.LOCALE_COUNTRY.isLocale()).isTrue();
        assertThat(CodeGroupSource.LOCALE_LANGUAGE.isLocale()).isTrue();
        assertThat(CodeGroupSource.DYNAMIC_DB.isLocale()).isFalse();
        assertThat(CodeGroupSource.STATIC_ENUM.isLocale()).isFalse();
        assertThat(CodeGroupSource.DW.isLocale()).isFalse();
        assertThat(CodeGroupSource.APPROVAL_GROUP.isLocale()).isFalse();
    }

    // ========== isRestoreOnDelete 테스트 ==========

    @Test
    @DisplayName("LOCALE_COUNTRY와 LOCALE_LANGUAGE는 삭제 시 원복된다")
    void restoreOnDeleteSources() {
        assertThat(CodeGroupSource.LOCALE_COUNTRY.isRestoreOnDelete()).isTrue();
        assertThat(CodeGroupSource.LOCALE_LANGUAGE.isRestoreOnDelete()).isTrue();
        assertThat(CodeGroupSource.STATIC_ENUM.isRestoreOnDelete()).isFalse();
        assertThat(CodeGroupSource.DYNAMIC_DB.isRestoreOnDelete()).isFalse();
        assertThat(CodeGroupSource.DW.isRestoreOnDelete()).isFalse();
        assertThat(CodeGroupSource.APPROVAL_GROUP.isRestoreOnDelete()).isFalse();
    }

    // ========== LOCALE 소스 수정 가능 테스트 ==========

    @Test
    @DisplayName("LOCALE_COUNTRY는 수정 가능하다")
    void localeCountryIsEditable() {
        assertThat(CodeGroupSource.LOCALE_COUNTRY.isEditable()).isTrue();
    }

    @Test
    @DisplayName("LOCALE_LANGUAGE는 수정 가능하다")
    void localeLanguageIsEditable() {
        assertThat(CodeGroupSource.LOCALE_LANGUAGE.isEditable()).isTrue();
    }

    @Test
    @DisplayName("LOCALE 소스는 읽기 전용이 아니다")
    void localeSourcesAreNotReadOnly() {
        assertThat(CodeGroupSource.LOCALE_COUNTRY.isReadOnly()).isFalse();
        assertThat(CodeGroupSource.LOCALE_LANGUAGE.isReadOnly()).isFalse();
    }

    @Test
    @DisplayName("LOCALE 소스는 DB 저장이 필요하다")
    void localeSourcesRequireDbStorage() {
        assertThat(CodeGroupSource.LOCALE_COUNTRY.isRequiresDbStorage()).isTrue();
        assertThat(CodeGroupSource.LOCALE_LANGUAGE.isRequiresDbStorage()).isTrue();
    }
}
