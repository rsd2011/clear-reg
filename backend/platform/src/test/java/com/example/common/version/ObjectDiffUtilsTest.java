package com.example.common.version;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.javers.core.diff.Diff;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ObjectDiffUtils 유틸리티")
class ObjectDiffUtilsTest {

    // 테스트용 샘플 클래스
    static class SampleEntity {
        private String name;
        private int displayOrder;
        private boolean active;
        private String description;

        SampleEntity(String name, int displayOrder, boolean active, String description) {
            this.name = name;
            this.displayOrder = displayOrder;
            this.active = active;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public int getDisplayOrder() {
            return displayOrder;
        }

        public boolean isActive() {
            return active;
        }

        public String getDescription() {
            return description;
        }
    }

    @Nested
    @DisplayName("compareFields")
    class CompareFields {

        @Test
        @DisplayName("Given: 동일한 객체 / When: compareFields 호출 / Then: 빈 리스트 반환")
        void identicalObjectsReturnEmptyList() {
            SampleEntity obj1 = new SampleEntity("Test", 1, true, "Desc");
            SampleEntity obj2 = new SampleEntity("Test", 1, true, "Desc");
            Map<String, String> labels = Map.of("name", "이름", "displayOrder", "순서");

            List<FieldDiff> diffs = ObjectDiffUtils.compareFields(obj1, obj2, labels);

            assertThat(diffs).isEmpty();
        }

        @Test
        @DisplayName("Given: name이 다른 두 객체 / When: compareFields 호출 / Then: name 변경 감지")
        void detectsNameChange() {
            SampleEntity old = new SampleEntity("Old", 1, true, "Desc");
            SampleEntity newObj = new SampleEntity("New", 1, true, "Desc");
            Map<String, String> labels = Map.of("name", "이름");

            List<FieldDiff> diffs = ObjectDiffUtils.compareFields(old, newObj, labels);

            assertThat(diffs).hasSize(1);
            FieldDiff diff = diffs.get(0);
            assertThat(diff.fieldName()).isEqualTo("name");
            assertThat(diff.fieldLabel()).isEqualTo("이름");
            assertThat(diff.beforeValue()).isEqualTo("Old");
            assertThat(diff.afterValue()).isEqualTo("New");
            assertThat(diff.diffType()).isEqualTo(DiffType.MODIFIED);
        }

        @Test
        @DisplayName("Given: 여러 필드가 다른 두 객체 / When: compareFields 호출 / Then: 레이블에 포함된 필드만 반환")
        void onlyReturnsLabeledFields() {
            SampleEntity old = new SampleEntity("Old", 1, true, "OldDesc");
            SampleEntity newObj = new SampleEntity("New", 2, false, "NewDesc");
            Map<String, String> labels = Map.of("name", "이름", "displayOrder", "순서");
            // active와 description은 레이블에 없으므로 결과에 포함되지 않아야 함

            List<FieldDiff> diffs = ObjectDiffUtils.compareFields(old, newObj, labels);

            assertThat(diffs).hasSize(2);
            assertThat(diffs).extracting(FieldDiff::fieldName)
                    .containsExactlyInAnyOrder("name", "displayOrder");
        }

        @Test
        @DisplayName("Given: 모든 필드가 변경됨 / When: compareFields 호출 / Then: 모든 레이블된 필드 반환")
        void returnsAllChangedLabeledFields() {
            SampleEntity old = new SampleEntity("Old", 1, true, "OldDesc");
            SampleEntity newObj = new SampleEntity("New", 2, false, "NewDesc");
            Map<String, String> labels = Map.of(
                    "name", "이름",
                    "displayOrder", "순서",
                    "active", "활성화",
                    "description", "설명"
            );

            List<FieldDiff> diffs = ObjectDiffUtils.compareFields(old, newObj, labels);

            assertThat(diffs).hasSize(4);
        }

        @Test
        @DisplayName("Given: 빈 레이블 맵 / When: compareFields 호출 / Then: 빈 리스트 반환")
        void emptyLabelsReturnsEmptyList() {
            SampleEntity old = new SampleEntity("Old", 1, true, "Desc");
            SampleEntity newObj = new SampleEntity("New", 2, false, "NewDesc");
            Map<String, String> labels = Map.of();

            List<FieldDiff> diffs = ObjectDiffUtils.compareFields(old, newObj, labels);

            assertThat(diffs).isEmpty();
        }
    }

    @Nested
    @DisplayName("compare")
    class Compare {

        @Test
        @DisplayName("Given: 두 객체 / When: compare 호출 / Then: Javers Diff 반환")
        void returnsJaversDiff() {
            SampleEntity old = new SampleEntity("Old", 1, true, "Desc");
            SampleEntity newObj = new SampleEntity("New", 1, true, "Desc");

            Diff diff = ObjectDiffUtils.compare(old, newObj);

            assertThat(diff).isNotNull();
            assertThat(diff.hasChanges()).isTrue();
        }

        @Test
        @DisplayName("Given: 동일한 객체 / When: compare 호출 / Then: 변경 없는 Diff 반환")
        void identicalObjectsReturnNoChanges() {
            SampleEntity obj1 = new SampleEntity("Test", 1, true, "Desc");
            SampleEntity obj2 = new SampleEntity("Test", 1, true, "Desc");

            Diff diff = ObjectDiffUtils.compare(obj1, obj2);

            assertThat(diff.hasChanges()).isFalse();
        }
    }

    @Nested
    @DisplayName("compareCollections")
    class CompareCollections {

        @Test
        @DisplayName("Given: 두 리스트 / When: compareCollections 호출 / Then: Javers Diff 반환")
        void returnsJaversDiffForCollections() {
            List<SampleEntity> oldList = List.of(
                    new SampleEntity("A", 1, true, "DescA")
            );
            List<SampleEntity> newList = List.of(
                    new SampleEntity("A", 1, true, "DescA"),
                    new SampleEntity("B", 2, true, "DescB")
            );

            Diff diff = ObjectDiffUtils.compareCollections(oldList, newList, SampleEntity.class);

            assertThat(diff).isNotNull();
            assertThat(diff.hasChanges()).isTrue();
        }

        @Test
        @DisplayName("Given: 동일한 리스트 / When: compareCollections 호출 / Then: 변경 없는 Diff 반환")
        void identicalCollectionsReturnNoChanges() {
            List<SampleEntity> list1 = List.of(
                    new SampleEntity("A", 1, true, "DescA")
            );
            List<SampleEntity> list2 = List.of(
                    new SampleEntity("A", 1, true, "DescA")
            );

            Diff diff = ObjectDiffUtils.compareCollections(list1, list2, SampleEntity.class);

            assertThat(diff.hasChanges()).isFalse();
        }

        @Test
        @DisplayName("Given: 빈 리스트와 요소가 있는 리스트 / When: compareCollections 호출 / Then: 추가 변경 감지")
        void detectsAdditionsToCollection() {
            List<SampleEntity> emptyList = List.of();
            List<SampleEntity> newList = List.of(
                    new SampleEntity("A", 1, true, "DescA")
            );

            Diff diff = ObjectDiffUtils.compareCollections(emptyList, newList, SampleEntity.class);

            assertThat(diff.hasChanges()).isTrue();
        }
    }
}
