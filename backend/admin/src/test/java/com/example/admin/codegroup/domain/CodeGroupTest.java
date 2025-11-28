package com.example.admin.codegroup.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CodeGroupTest {

    @Nested
    @DisplayName("CodeGroup 생성 테스트")
    class CreateTests {

        @Test
        @DisplayName("Given: 동적 그룹 정보 / When: createDynamic 호출 / Then: DYNAMIC_DB 소스로 그룹이 생성된다")
        void createDynamic() {
            // Given & When
            CodeGroup group = CodeGroup.createDynamic(
                    "CUSTOM_CODE",
                    "사용자 정의 코드",
                    "테스트용 코드 그룹",
                    "admin"
            );

            // Then
            assertThat(group.getSource()).isEqualTo(CodeGroupSource.DYNAMIC_DB);
            assertThat(group.getGroupCode()).isEqualTo("CUSTOM_CODE");
            assertThat(group.getGroupName()).isEqualTo("사용자 정의 코드");
            assertThat(group.getDescription()).isEqualTo("테스트용 코드 그룹");
            assertThat(group.isDynamic()).isTrue();
            assertThat(group.isStatic()).isFalse();
            assertThat(group.isActive()).isTrue();
            assertThat(group.getUpdatedBy()).isEqualTo("admin");
        }

        @Test
        @DisplayName("Given: 소문자 그룹 코드 / When: createDynamic 호출 / Then: 대문자로 정규화된다")
        void createDynamicNormalizesCode() {
            // Given & When
            CodeGroup group = CodeGroup.createDynamic(
                    "custom_code",
                    "사용자 정의 코드",
                    null,
                    "admin"
            );

            // Then
            assertThat(group.getGroupCode()).isEqualTo("CUSTOM_CODE");
        }

        @Test
        @DisplayName("Given: 정적 오버라이드 정보 / When: createStaticOverride 호출 / Then: STATIC_ENUM 소스로 그룹이 생성된다")
        void createStaticOverride() {
            // Given & When
            CodeGroup group = CodeGroup.createStaticOverride(
                    "FILE_STATUS",
                    "파일 상태",
                    "Enum 오버라이드용",
                    "admin"
            );

            // Then
            assertThat(group.getSource()).isEqualTo(CodeGroupSource.STATIC_ENUM);
            assertThat(group.getGroupCode()).isEqualTo("FILE_STATUS");
            assertThat(group.isStatic()).isTrue();
            assertThat(group.isDynamic()).isFalse();
            assertThat(group.isActive()).isTrue();
        }

        @Test
        @DisplayName("Given: 전체 파라미터 / When: create 호출 / Then: 지정된 값으로 그룹이 생성된다")
        void createWithAllParams() {
            // Given & When
            CodeGroup group = CodeGroup.create(
                    CodeGroupSource.DW,
                    "DW_CODE",
                    "DW 코드",
                    "DW에서 수집된 코드",
                    true,
                    "{\"category\":\"system\"}",
                    10,
                    "batch",
                    null
            );

            // Then
            assertThat(group.getSource()).isEqualTo(CodeGroupSource.DW);
            assertThat(group.getGroupCode()).isEqualTo("DW_CODE");
            assertThat(group.getMetadataJson()).isEqualTo("{\"category\":\"system\"}");
            assertThat(group.getDisplayOrder()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("CodeGroup 수정 테스트")
    class UpdateTests {

        @Test
        @DisplayName("Given: 기존 그룹 / When: update 호출 / Then: 정보가 업데이트된다")
        void update() {
            // Given
            CodeGroup group = CodeGroup.createDynamic("CODE", "이름", "설명", "admin");

            // When
            group.update("새 이름", "새 설명", false, "{\"key\":\"value\"}", 5, "modifier", null);

            // Then
            assertThat(group.getGroupName()).isEqualTo("새 이름");
            assertThat(group.getDescription()).isEqualTo("새 설명");
            assertThat(group.isActive()).isFalse();
            assertThat(group.getMetadataJson()).isEqualTo("{\"key\":\"value\"}");
            assertThat(group.getDisplayOrder()).isEqualTo(5);
            assertThat(group.getUpdatedBy()).isEqualTo("modifier");
        }

        @Test
        @DisplayName("Given: 기존 그룹 / When: changeGroupCode 호출 / Then: 그룹 코드가 변경된다")
        void changeGroupCode() {
            // Given
            CodeGroup group = CodeGroup.createDynamic("OLD_CODE", "이름", null, "admin");

            // When
            group.changeGroupCode("new_code");

            // Then
            assertThat(group.getGroupCode()).isEqualTo("NEW_CODE");
        }
    }

    @Nested
    @DisplayName("CodeGroup 아이템 관리 테스트")
    class ItemManagementTests {

        @Test
        @DisplayName("Given: 그룹 / When: addItem 호출 / Then: 아이템이 추가된다")
        void addItem() {
            // Given
            CodeGroup group = CodeGroup.createDynamic("LANG", "언어", null, "admin");

            // When
            CodeItem item = group.addItem("KO", "한국어", 0, true, "한국어 코드", null, "admin");

            // Then
            assertThat(group.getItems()).hasSize(1);
            assertThat(item.getItemCode()).isEqualTo("KO");
            assertThat(item.getItemName()).isEqualTo("한국어");
            assertThat(item.getCodeGroup()).isEqualTo(group);
        }

        @Test
        @DisplayName("Given: 아이템이 있는 그룹 / When: removeItem 호출 / Then: 아이템이 제거된다")
        void removeItem() {
            // Given
            CodeGroup group = CodeGroup.createDynamic("LANG", "언어", null, "admin");
            CodeItem item = group.addItem("KO", "한국어", 0, true, null, null, "admin");

            // When
            boolean removed = group.removeItem(item);

            // Then
            assertThat(removed).isTrue();
            assertThat(group.getItems()).isEmpty();
        }

        @Test
        @DisplayName("Given: 여러 아이템이 있는 그룹 / When: findItem 호출 / Then: 해당 아이템을 찾는다")
        void findItem() {
            // Given
            CodeGroup group = CodeGroup.createDynamic("LANG", "언어", null, "admin");
            group.addItem("KO", "한국어", 0, true, null, null, "admin");
            group.addItem("EN", "영어", 1, true, null, null, "admin");
            group.addItem("JA", "일본어", 2, true, null, null, "admin");

            // When
            CodeItem found = group.findItem("EN");

            // Then
            assertThat(found).isNotNull();
            assertThat(found.getItemName()).isEqualTo("영어");
        }

        @Test
        @DisplayName("Given: 아이템이 있는 그룹 / When: 존재하지 않는 코드로 findItem 호출 / Then: null을 반환한다")
        void findItemReturnsNullWhenNotFound() {
            // Given
            CodeGroup group = CodeGroup.createDynamic("LANG", "언어", null, "admin");
            group.addItem("KO", "한국어", 0, true, null, null, "admin");

            // When
            CodeItem found = group.findItem("NONEXISTENT");

            // Then
            assertThat(found).isNull();
        }
    }

    @Nested
    @DisplayName("CodeGroup 유틸리티 테스트")
    class UtilityTests {

        @Test
        @DisplayName("Given: DYNAMIC_DB 소스 그룹 / When: isEditable 호출 / Then: true를 반환한다")
        void isEditableTrueForDynamicDb() {
            CodeGroup group = CodeGroup.createDynamic("CODE", "이름", null, "admin");
            assertThat(group.isEditable()).isTrue();
        }

        @Test
        @DisplayName("Given: STATIC_ENUM 소스 그룹 / When: isEditable 호출 / Then: true를 반환한다 (기존 Enum 값 오버라이드 가능)")
        void isEditableTrueForStaticEnum() {
            CodeGroup group = CodeGroup.createStaticOverride("CODE", "이름", null, "admin");
            assertThat(group.isEditable()).isTrue();
        }

        @Test
        @DisplayName("Given: 동적 그룹 / When: isDynamic 호출 / Then: true를 반환한다")
        void isDynamicForDynamicDb() {
            CodeGroup group = CodeGroup.createDynamic("CODE", "이름", null, "admin");
            assertThat(group.isDynamic()).isTrue();
            assertThat(group.isStatic()).isFalse();
        }

        @Test
        @DisplayName("Given: 정적 그룹 / When: isStatic 호출 / Then: true를 반환한다")
        void isStaticForStaticEnum() {
            CodeGroup group = CodeGroup.createStaticOverride("CODE", "이름", null, "admin");
            assertThat(group.isStatic()).isTrue();
            assertThat(group.isDynamic()).isFalse();
        }

        @Test
        @DisplayName("Given: 그룹 / When: copy 호출 / Then: 동일한 값을 가진 복사본이 생성된다")
        void copy() {
            // Given
            CodeGroup original = CodeGroup.createDynamic("CODE", "이름", "설명", "admin");

            // When
            CodeGroup copy = original.copy();

            // Then
            assertThat(copy).isNotSameAs(original);
            assertThat(copy.getGroupCode()).isEqualTo(original.getGroupCode());
            assertThat(copy.getGroupName()).isEqualTo(original.getGroupName());
            assertThat(copy.getDescription()).isEqualTo(original.getDescription());
            assertThat(copy.getSource()).isEqualTo(original.getSource());
        }
    }
}
