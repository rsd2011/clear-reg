package com.example.server.codegroup;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.admin.codegroup.dto.CodeGroupItem;

@DisplayName("DefaultStaticCodeRegistry 테스트")
class DefaultStaticCodeRegistryTest {

    private DefaultStaticCodeRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DefaultStaticCodeRegistry();
        registry.init();
    }

    @Nested
    @DisplayName("@ManagedCode 어노테이션 스캔")
    class ManagedCodeScanningTest {

        @Test
        @DisplayName("Given: @ManagedCode 어노테이션이 적용된 Enum / When: init() 호출 / Then: 레지스트리에 등록된다")
        void scansManagedCodeAnnotatedEnums() {
            // When
            Set<String> groupCodes = registry.getGroupCodes();

            // Then
            assertThat(groupCodes).contains("TEST_MANAGED_ENUM");
        }

        @Test
        @DisplayName("Given: hidden=true 설정된 Enum / When: init() 호출 / Then: 레지스트리에 등록되지 않는다")
        void excludesHiddenEnums() {
            // When
            Set<String> groupCodes = registry.getGroupCodes();

            // Then
            assertThat(groupCodes).doesNotContain("TEST_HIDDEN_ENUM");
        }

        @Test
        @DisplayName("Given: 등록된 Enum / When: getRegisteredEnums() 호출 / Then: Enum 클래스가 반환된다")
        void returnsRegisteredEnumClasses() {
            // When
            Set<Class<? extends Enum<?>>> enums = registry.getRegisteredEnums();

            // Then
            assertThat(enums).contains(TestManagedEnum.class);
            assertThat(enums).doesNotContain(TestHiddenEnum.class);
        }
    }

    @Nested
    @DisplayName("@CodeValue 어노테이션 처리")
    class CodeValueProcessingTest {

        @Test
        @DisplayName("Given: @CodeValue 어노테이션이 적용된 상수 / When: getItems() 호출 / Then: 라벨과 설명이 적용된다")
        void appliesCodeValueAnnotation() {
            // When
            List<CodeGroupItem> items = registry.getItems("TEST_MANAGED_ENUM");

            // Then
            assertThat(items).hasSize(4);

            CodeGroupItem pending = items.stream()
                    .filter(i -> "PENDING".equals(i.itemCode()))
                    .findFirst()
                    .orElseThrow();
            assertThat(pending.itemName()).isEqualTo("대기 중");
            assertThat(pending.description()).isEqualTo("처리 대기 상태");
            assertThat(pending.displayOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("Given: order가 설정된 상수들 / When: getItems() 호출 / Then: displayOrder 순으로 정렬된다")
        void sortsItemsByDisplayOrder() {
            // When
            List<CodeGroupItem> items = registry.getItems("TEST_MANAGED_ENUM");

            // Then
            assertThat(items).extracting(CodeGroupItem::itemCode)
                    .containsExactly("PENDING", "IN_PROGRESS", "COMPLETED", "CANCELLED");
        }
    }

    @Nested
    @DisplayName("조회 메소드")
    class QueryMethodsTest {

        @Test
        @DisplayName("Given: 등록된 그룹코드 / When: hasGroup() 호출 / Then: true 반환")
        void hasGroupReturnsTrueForRegisteredGroup() {
            assertThat(registry.hasGroup("TEST_MANAGED_ENUM")).isTrue();
            assertThat(registry.hasGroup("test_managed_enum")).isTrue(); // 대소문자 무시
        }

        @Test
        @DisplayName("Given: 미등록 그룹코드 / When: hasGroup() 호출 / Then: false 반환")
        void hasGroupReturnsFalseForUnregisteredGroup() {
            assertThat(registry.hasGroup("NON_EXISTENT")).isFalse();
        }

        @Test
        @DisplayName("Given: 등록된 그룹코드 / When: getEnumClass() 호출 / Then: Enum 클래스 반환")
        void getEnumClassReturnsEnumForRegisteredGroup() {
            // When
            Optional<Class<? extends Enum<?>>> enumClass = registry.getEnumClass("TEST_MANAGED_ENUM");

            // Then
            assertThat(enumClass).isPresent();
            assertThat(enumClass.get()).isEqualTo(TestManagedEnum.class);
        }

        @Test
        @DisplayName("Given: 등록된 아이템 / When: getItem() 호출 / Then: 해당 아이템 반환")
        void getItemReturnsSpecificItem() {
            // When
            Optional<CodeGroupItem> item = registry.getItem("TEST_MANAGED_ENUM", "COMPLETED");

            // Then
            assertThat(item).isPresent();
            assertThat(item.get().itemCode()).isEqualTo("COMPLETED");
            assertThat(item.get().itemName()).isEqualTo("완료");
        }

        @Test
        @DisplayName("Given: 모든 등록된 Enum / When: getAllItems() 호출 / Then: 전체 아이템 맵 반환")
        void getAllItemsReturnsAllRegisteredItems() {
            // When
            Map<String, List<CodeGroupItem>> allItems = registry.getAllItems();

            // Then
            assertThat(allItems).containsKey("TEST_MANAGED_ENUM");
            assertThat(allItems.get("TEST_MANAGED_ENUM")).hasSize(4);
        }
    }

    @Nested
    @DisplayName("캐시 관리")
    class CacheManagementTest {

        @Test
        @DisplayName("Given: 캐시된 아이템 / When: invalidateCache(groupCode) 호출 / Then: 해당 캐시만 무효화")
        void invalidateCacheForSpecificGroup() {
            // Given - 캐시 생성
            registry.getItems("TEST_MANAGED_ENUM");

            // When
            registry.invalidateCache("TEST_MANAGED_ENUM");

            // Then - 다시 조회해도 정상 동작
            List<CodeGroupItem> items = registry.getItems("TEST_MANAGED_ENUM");
            assertThat(items).hasSize(4);
        }

        @Test
        @DisplayName("Given: 캐시된 아이템들 / When: invalidateCache(null) 호출 / Then: 전체 캐시 무효화")
        void invalidateAllCaches() {
            // Given - 캐시 생성
            registry.getItems("TEST_MANAGED_ENUM");

            // When
            registry.invalidateCache(null);

            // Then - 다시 조회해도 정상 동작
            List<CodeGroupItem> items = registry.getItems("TEST_MANAGED_ENUM");
            assertThat(items).hasSize(4);
        }
    }

    @Nested
    @DisplayName("Enum 클래스 직접 조회")
    class DirectEnumQueryTest {

        @Test
        @DisplayName("Given: Enum 클래스 / When: getCodeGroupItems(Class) 호출 / Then: 아이템 목록 반환")
        void getCodeGroupItemsFromEnumClass() {
            // When
            List<CodeGroupItem> items = registry.getCodeGroupItems(TestManagedEnum.class);

            // Then
            assertThat(items).hasSize(4);
            assertThat(items).extracting(CodeGroupItem::itemCode)
                    .containsExactly("PENDING", "IN_PROGRESS", "COMPLETED", "CANCELLED");
        }
    }
}
