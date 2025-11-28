package com.example.admin.codegroup.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CodeItemTest {

    @Nested
    @DisplayName("CodeItem 생성 테스트")
    class CreateTests {

        @Test
        @DisplayName("Given: 그룹과 아이템 정보 / When: create 호출 / Then: 아이템이 생성된다")
        void create() {
            // Given
            CodeGroup group = CodeGroup.createDynamic("LANG", "언어", null, "admin");

            // When
            CodeItem item = CodeItem.create(
                    group,
                    "KO",
                    "한국어",
                    0,
                    true,
                    "한국어 코드",
                    "{\"locale\":\"ko_KR\"}",
                    "admin",
                    null
            );

            // Then
            assertThat(item.getCodeGroup()).isEqualTo(group);
            assertThat(item.getGroupCode()).isEqualTo("LANG");
            assertThat(item.getItemCode()).isEqualTo("KO");
            assertThat(item.getItemName()).isEqualTo("한국어");
            assertThat(item.getDisplayOrder()).isEqualTo(0);
            assertThat(item.isActive()).isTrue();
            assertThat(item.getDescription()).isEqualTo("한국어 코드");
            assertThat(item.getMetadataJson()).isEqualTo("{\"locale\":\"ko_KR\"}");
            assertThat(item.getUpdatedBy()).isEqualTo("admin");
        }

        @Test
        @DisplayName("Given: 비활성 아이템 정보 / When: create 호출 / Then: 비활성 상태로 생성된다")
        void createInactive() {
            // Given
            CodeGroup group = CodeGroup.createDynamic("STATUS", "상태", null, "admin");

            // When
            CodeItem item = CodeItem.create(group, "INACTIVE", "비활성", 0, false, null, null, "admin", null);

            // Then
            assertThat(item.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("CodeItem 수정 테스트")
    class UpdateTests {

        @Test
        @DisplayName("Given: 기존 아이템 / When: update 호출 / Then: 정보가 업데이트된다")
        void update() {
            // Given
            CodeGroup group = CodeGroup.createDynamic("LANG", "언어", null, "admin");
            CodeItem item = CodeItem.create(group, "KO", "한국어", 0, true, null, null, "admin", null);

            // When
            item.update("한글", 5, false, "한글 설명", "{\"key\":\"value\"}", "modifier", null);

            // Then
            assertThat(item.getItemName()).isEqualTo("한글");
            assertThat(item.getDisplayOrder()).isEqualTo(5);
            assertThat(item.isActive()).isFalse();
            assertThat(item.getDescription()).isEqualTo("한글 설명");
            assertThat(item.getMetadataJson()).isEqualTo("{\"key\":\"value\"}");
            assertThat(item.getUpdatedBy()).isEqualTo("modifier");
        }

        @Test
        @DisplayName("Given: 기존 아이템 / When: update로 활성화 / Then: 활성 상태로 변경된다")
        void updateActivate() {
            // Given
            CodeGroup group = CodeGroup.createDynamic("STATUS", "상태", null, "admin");
            CodeItem item = CodeItem.create(group, "INACTIVE", "비활성", 0, false, null, null, "admin", null);

            // When
            item.update("활성화됨", 0, true, null, null, "modifier", null);

            // Then
            assertThat(item.isActive()).isTrue();
            assertThat(item.getItemName()).isEqualTo("활성화됨");
        }
    }

    @Nested
    @DisplayName("CodeItem 유틸리티 테스트")
    class UtilityTests {

        @Test
        @DisplayName("Given: 그룹에 연결된 아이템 / When: getSource 호출 / Then: 그룹의 소스를 반환한다")
        void getSourceFromGroup() {
            // Given
            CodeGroup group = CodeGroup.createDynamic("LANG", "언어", null, "admin");
            CodeItem item = CodeItem.create(group, "KO", "한국어", 0, true, null, null, "admin", null);

            // Then
            assertThat(item.getSource()).isEqualTo(CodeGroupSource.DYNAMIC_DB);
        }

        @Test
        @DisplayName("Given: STATIC_ENUM 그룹의 아이템 / When: getSource 호출 / Then: STATIC_ENUM을 반환한다")
        void getSourceFromStaticEnumGroup() {
            // Given
            CodeGroup group = CodeGroup.createStaticOverride("STATUS", "상태", null, "admin");
            CodeItem item = CodeItem.create(group, "ACTIVE", "활성", 0, true, null, null, "admin", null);

            // Then
            assertThat(item.getSource()).isEqualTo(CodeGroupSource.STATIC_ENUM);
        }

        @Test
        @DisplayName("Given: DYNAMIC_DB 소스 아이템 / When: isEditable 호출 / Then: true를 반환한다")
        void isEditableTrueForDynamicDb() {
            CodeGroup group = CodeGroup.createDynamic("CODE", "이름", null, "admin");
            CodeItem item = CodeItem.create(group, "V1", "값1", 0, true, null, null, "admin", null);

            assertThat(item.isEditable()).isTrue();
        }

        @Test
        @DisplayName("Given: STATIC_ENUM 소스 아이템 / When: isEditable 호출 / Then: true를 반환한다 (기존 Enum 값 오버라이드 가능)")
        void isEditableTrueForStaticEnum() {
            CodeGroup group = CodeGroup.createStaticOverride("STATUS", "상태", null, "admin");
            CodeItem item = CodeItem.create(group, "ACTIVE", "활성", 0, true, null, null, "admin", null);

            assertThat(item.isEditable()).isTrue();
        }

        @Test
        @DisplayName("Given: DYNAMIC_DB 소스 아이템 / When: isDeletable 호출 / Then: true를 반환한다")
        void isDeletableTrueForDynamicDb() {
            CodeGroup group = CodeGroup.createDynamic("CODE", "이름", null, "admin");
            CodeItem item = CodeItem.create(group, "V1", "값1", 0, true, null, null, "admin", null);

            assertThat(item.isDeletable()).isTrue();
        }

        @Test
        @DisplayName("Given: STATIC_ENUM 소스 아이템 / When: isDeletable 호출 / Then: true를 반환한다 (STATIC_ENUM도 삭제 가능)")
        void isDeletableTrueForStaticEnum() {
            CodeGroup group = CodeGroup.createStaticOverride("STATUS", "상태", null, "admin");
            CodeItem item = CodeItem.create(group, "ACTIVE", "활성", 0, true, null, null, "admin", null);

            assertThat(item.isDeletable()).isTrue();
        }

        @Test
        @DisplayName("Given: DW 소스 아이템 / When: isDeletable 호출 / Then: false를 반환한다")
        void isDeletableFalseForDw() {
            CodeGroup group = CodeGroup.create(
                    CodeGroupSource.DW, "DW_CODE", "DW 코드", null,
                    true, null, 0, "system", null);
            CodeItem item = CodeItem.create(group, "V1", "값1", 0, true, null, null, "admin", null);

            assertThat(item.isDeletable()).isFalse();
        }

        @Test
        @DisplayName("Given: 아이템 / When: copy 호출 / Then: 동일한 값을 가진 복사본이 생성된다")
        void copy() {
            // Given
            CodeGroup group = CodeGroup.createDynamic("LANG", "언어", null, "admin");
            CodeItem original = CodeItem.create(group, "KO", "한국어", 0, true, "설명", null, "admin", null);

            // When
            CodeItem copy = original.copy();

            // Then
            assertThat(copy).isNotSameAs(original);
            assertThat(copy.getItemCode()).isEqualTo(original.getItemCode());
            assertThat(copy.getItemName()).isEqualTo(original.getItemName());
            assertThat(copy.getDescription()).isEqualTo(original.getDescription());
            assertThat(copy.getCodeGroup()).isEqualTo(original.getCodeGroup());
        }

        @Test
        @DisplayName("Given: 그룹 코드 / When: getGroupCode 호출 / Then: 그룹의 코드를 반환한다")
        void getGroupCode() {
            // Given
            CodeGroup group = CodeGroup.createDynamic("MY_GROUP", "내 그룹", null, "admin");
            CodeItem item = CodeItem.create(group, "V1", "값1", 0, true, null, null, "admin", null);

            // Then
            assertThat(item.getGroupCode()).isEqualTo("MY_GROUP");
        }
    }
}
