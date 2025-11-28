package com.example.common.version;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("FieldDiff 레코드")
class FieldDiffTest {

    @Nested
    @DisplayName("레코드 생성")
    class Constructor {

        @Test
        @DisplayName("Given: 모든 필드 / When: 생성자 호출 / Then: 모든 필드가 올바르게 설정됨")
        void constructorSetsAllFields() {
            FieldDiff diff = new FieldDiff("name", "이름", "old", "new", DiffType.MODIFIED);

            assertThat(diff.fieldName()).isEqualTo("name");
            assertThat(diff.fieldLabel()).isEqualTo("이름");
            assertThat(diff.beforeValue()).isEqualTo("old");
            assertThat(diff.afterValue()).isEqualTo("new");
            assertThat(diff.diffType()).isEqualTo(DiffType.MODIFIED);
        }

        @Test
        @DisplayName("Given: null 값 포함 / When: 생성자 호출 / Then: null 값이 그대로 저장됨")
        void constructorAllowsNullValues() {
            FieldDiff diff = new FieldDiff("active", "활성화", null, true, DiffType.ADDED);

            assertThat(diff.beforeValue()).isNull();
            assertThat(diff.afterValue()).isEqualTo(true);
        }
    }

    @Nested
    @DisplayName("팩토리 메서드")
    class FactoryMethods {

        @Test
        @DisplayName("Given: 수정된 필드 정보 / When: modified() 호출 / Then: MODIFIED 타입 FieldDiff 반환")
        void modifiedCreatesModifiedDiff() {
            FieldDiff diff = FieldDiff.modified("name", "이름", "old", "new");

            assertThat(diff.fieldName()).isEqualTo("name");
            assertThat(diff.fieldLabel()).isEqualTo("이름");
            assertThat(diff.beforeValue()).isEqualTo("old");
            assertThat(diff.afterValue()).isEqualTo("new");
            assertThat(diff.diffType()).isEqualTo(DiffType.MODIFIED);
        }

        @Test
        @DisplayName("Given: 추가된 필드 정보 / When: added() 호출 / Then: ADDED 타입 FieldDiff 반환, beforeValue는 null")
        void addedCreatesAddedDiff() {
            FieldDiff diff = FieldDiff.added("active", "활성화", true);

            assertThat(diff.fieldName()).isEqualTo("active");
            assertThat(diff.fieldLabel()).isEqualTo("활성화");
            assertThat(diff.beforeValue()).isNull();
            assertThat(diff.afterValue()).isEqualTo(true);
            assertThat(diff.diffType()).isEqualTo(DiffType.ADDED);
        }

        @Test
        @DisplayName("Given: 삭제된 필드 정보 / When: removed() 호출 / Then: REMOVED 타입 FieldDiff 반환, afterValue는 null")
        void removedCreatesRemovedDiff() {
            FieldDiff diff = FieldDiff.removed("order", "순서", 5);

            assertThat(diff.fieldName()).isEqualTo("order");
            assertThat(diff.fieldLabel()).isEqualTo("순서");
            assertThat(diff.beforeValue()).isEqualTo(5);
            assertThat(diff.afterValue()).isNull();
            assertThat(diff.diffType()).isEqualTo(DiffType.REMOVED);
        }
    }

    @Nested
    @DisplayName("동등성")
    class Equality {

        @Test
        @DisplayName("Given: 동일한 값의 두 FieldDiff / When: equals() 호출 / Then: true 반환")
        void equalDiffsAreEqual() {
            FieldDiff diff1 = new FieldDiff("name", "이름", "old", "new", DiffType.MODIFIED);
            FieldDiff diff2 = new FieldDiff("name", "이름", "old", "new", DiffType.MODIFIED);

            assertThat(diff1).isEqualTo(diff2);
            assertThat(diff1.hashCode()).isEqualTo(diff2.hashCode());
        }

        @Test
        @DisplayName("Given: 다른 값의 두 FieldDiff / When: equals() 호출 / Then: false 반환")
        void differentDiffsAreNotEqual() {
            FieldDiff diff1 = new FieldDiff("name", "이름", "old", "new", DiffType.MODIFIED);
            FieldDiff diff2 = new FieldDiff("name", "이름", "old", "different", DiffType.MODIFIED);

            assertThat(diff1).isNotEqualTo(diff2);
        }

        @Test
        @DisplayName("Given: FieldDiff 인스턴스 / When: toString() 호출 / Then: 모든 필드 포함")
        void toStringContainsAllFields() {
            FieldDiff diff = FieldDiff.modified("name", "이름", "old", "new");
            String str = diff.toString();

            assertThat(str).contains("name", "이름", "old", "new", "MODIFIED");
        }
    }
}
