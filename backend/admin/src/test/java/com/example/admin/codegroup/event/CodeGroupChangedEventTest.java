package com.example.admin.codegroup.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.admin.codegroup.domain.CodeGroupSource;
import com.example.admin.codegroup.event.CodeGroupChangedEvent.ChangeType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * CodeGroupChangedEvent 테스트.
 */
@DisplayName("CodeGroupChangedEvent 테스트")
class CodeGroupChangedEventTest {

    private final Object eventSource = this;

    @Nested
    @DisplayName("그룹 이벤트 팩토리 메서드")
    class GroupEventFactoryTests {

        @Test
        @DisplayName("Given: 그룹 정보 / When: groupCreated 호출 / Then: GROUP_CREATED 이벤트 생성")
        void createsGroupCreatedEvent() {
            // When
            CodeGroupChangedEvent event = CodeGroupChangedEvent.groupCreated(
                    eventSource, CodeGroupSource.DYNAMIC_DB, "LANG");

            // Then
            assertThat(event.getChangeType()).isEqualTo(ChangeType.GROUP_CREATED);
            // ApplicationEvent.getSource()는 이벤트 소스 객체 반환
            // CodeGroupSource는 필드명이 source이지만 Lombok getter가 getSource()로 생성됨 (충돌)
            // toString()에서 source(CodeGroupSource) 값 확인
            assertThat(event.toString()).contains("DYNAMIC_DB");
            assertThat(event.getGroupCode()).isEqualTo("LANG");
            assertThat(event.getItemCode()).isNull();
            assertThat(event.getNewGroupCode()).isNull();
        }

        @Test
        @DisplayName("Given: 그룹 정보 / When: groupUpdated 호출 / Then: GROUP_UPDATED 이벤트 생성")
        void createsGroupUpdatedEvent() {
            // When
            CodeGroupChangedEvent event = CodeGroupChangedEvent.groupUpdated(
                    eventSource, CodeGroupSource.STATIC_ENUM, "STATUS");

            // Then
            assertThat(event.getChangeType()).isEqualTo(ChangeType.GROUP_UPDATED);
            assertThat(event.getGroupCode()).isEqualTo("STATUS");
            assertThat(event.getItemCode()).isNull();
        }

        @Test
        @DisplayName("Given: 그룹 정보 / When: groupDeleted 호출 / Then: GROUP_DELETED 이벤트 생성")
        void createsGroupDeletedEvent() {
            // When
            CodeGroupChangedEvent event = CodeGroupChangedEvent.groupDeleted(
                    eventSource, CodeGroupSource.DYNAMIC_DB, "OLD_GROUP");

            // Then
            assertThat(event.getChangeType()).isEqualTo(ChangeType.GROUP_DELETED);
            assertThat(event.getGroupCode()).isEqualTo("OLD_GROUP");
        }
    }

    @Nested
    @DisplayName("아이템 이벤트 팩토리 메서드")
    class ItemEventFactoryTests {

        @Test
        @DisplayName("Given: 아이템 정보 / When: itemCreated 호출 / Then: ITEM_CREATED 이벤트 생성")
        void createsItemCreatedEvent() {
            // When
            CodeGroupChangedEvent event = CodeGroupChangedEvent.itemCreated(
                    eventSource, CodeGroupSource.DYNAMIC_DB, "LANG", "KO");

            // Then
            assertThat(event.getChangeType()).isEqualTo(ChangeType.ITEM_CREATED);
            assertThat(event.getGroupCode()).isEqualTo("LANG");
            assertThat(event.getItemCode()).isEqualTo("KO");
            assertThat(event.getNewGroupCode()).isNull();
        }

        @Test
        @DisplayName("Given: 아이템 정보 / When: itemUpdated 호출 / Then: ITEM_UPDATED 이벤트 생성")
        void createsItemUpdatedEvent() {
            // When
            CodeGroupChangedEvent event = CodeGroupChangedEvent.itemUpdated(
                    eventSource, CodeGroupSource.STATIC_ENUM, "APPROVAL_STATUS", "PENDING");

            // Then
            assertThat(event.getChangeType()).isEqualTo(ChangeType.ITEM_UPDATED);
            assertThat(event.getGroupCode()).isEqualTo("APPROVAL_STATUS");
            assertThat(event.getItemCode()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("Given: 아이템 정보 / When: itemDeleted 호출 / Then: ITEM_DELETED 이벤트 생성")
        void createsItemDeletedEvent() {
            // When
            CodeGroupChangedEvent event = CodeGroupChangedEvent.itemDeleted(
                    eventSource, CodeGroupSource.DYNAMIC_DB, "NOTICE_TYPE", "NT01");

            // Then
            assertThat(event.getChangeType()).isEqualTo(ChangeType.ITEM_DELETED);
            assertThat(event.getGroupCode()).isEqualTo("NOTICE_TYPE");
            assertThat(event.getItemCode()).isEqualTo("NT01");
        }
    }

    @Nested
    @DisplayName("마이그레이션 이벤트")
    class MigrationEventTests {

        @Test
        @DisplayName("Given: 마이그레이션 정보 / When: migrated 호출 / Then: MIGRATED 이벤트 생성")
        void createsMigratedEvent() {
            // When
            CodeGroupChangedEvent event = CodeGroupChangedEvent.migrated(
                    eventSource, CodeGroupSource.DYNAMIC_DB, "OLD_CODE", "NEW_CODE");

            // Then
            assertThat(event.getChangeType()).isEqualTo(ChangeType.MIGRATED);
            assertThat(event.getGroupCode()).isEqualTo("OLD_CODE");
            assertThat(event.getNewGroupCode()).isEqualTo("NEW_CODE");
            assertThat(event.getItemCode()).isNull();
        }
    }

    @Nested
    @DisplayName("이벤트 분류 메서드")
    class EventClassificationTests {

        @Test
        @DisplayName("Given: GROUP_CREATED / When: isGroupEvent 호출 / Then: true")
        void groupCreatedIsGroupEvent() {
            CodeGroupChangedEvent event = CodeGroupChangedEvent.groupCreated(
                    eventSource, CodeGroupSource.DYNAMIC_DB, "TEST");

            assertThat(event.isGroupEvent()).isTrue();
            assertThat(event.isItemEvent()).isFalse();
        }

        @Test
        @DisplayName("Given: GROUP_UPDATED / When: isGroupEvent 호출 / Then: true")
        void groupUpdatedIsGroupEvent() {
            CodeGroupChangedEvent event = CodeGroupChangedEvent.groupUpdated(
                    eventSource, CodeGroupSource.DYNAMIC_DB, "TEST");

            assertThat(event.isGroupEvent()).isTrue();
            assertThat(event.isItemEvent()).isFalse();
        }

        @Test
        @DisplayName("Given: GROUP_DELETED / When: isGroupEvent 호출 / Then: true")
        void groupDeletedIsGroupEvent() {
            CodeGroupChangedEvent event = CodeGroupChangedEvent.groupDeleted(
                    eventSource, CodeGroupSource.DYNAMIC_DB, "TEST");

            assertThat(event.isGroupEvent()).isTrue();
            assertThat(event.isItemEvent()).isFalse();
        }

        @Test
        @DisplayName("Given: ITEM_CREATED / When: isItemEvent 호출 / Then: true")
        void itemCreatedIsItemEvent() {
            CodeGroupChangedEvent event = CodeGroupChangedEvent.itemCreated(
                    eventSource, CodeGroupSource.DYNAMIC_DB, "TEST", "ITEM1");

            assertThat(event.isItemEvent()).isTrue();
            assertThat(event.isGroupEvent()).isFalse();
        }

        @Test
        @DisplayName("Given: ITEM_UPDATED / When: isItemEvent 호출 / Then: true")
        void itemUpdatedIsItemEvent() {
            CodeGroupChangedEvent event = CodeGroupChangedEvent.itemUpdated(
                    eventSource, CodeGroupSource.DYNAMIC_DB, "TEST", "ITEM1");

            assertThat(event.isItemEvent()).isTrue();
            assertThat(event.isGroupEvent()).isFalse();
        }

        @Test
        @DisplayName("Given: ITEM_DELETED / When: isItemEvent 호출 / Then: true")
        void itemDeletedIsItemEvent() {
            CodeGroupChangedEvent event = CodeGroupChangedEvent.itemDeleted(
                    eventSource, CodeGroupSource.DYNAMIC_DB, "TEST", "ITEM1");

            assertThat(event.isItemEvent()).isTrue();
            assertThat(event.isGroupEvent()).isFalse();
        }

        @Test
        @DisplayName("Given: MIGRATED / When: isGroupEvent/isItemEvent 호출 / Then: 둘 다 false")
        void migratedIsNeitherGroupNorItemEvent() {
            CodeGroupChangedEvent event = CodeGroupChangedEvent.migrated(
                    eventSource, CodeGroupSource.DYNAMIC_DB, "OLD", "NEW");

            assertThat(event.isGroupEvent()).isFalse();
            assertThat(event.isItemEvent()).isFalse();
        }
    }

    @Nested
    @DisplayName("toString 테스트")
    class ToStringTests {

        @Test
        @DisplayName("Given: 이벤트 / When: toString 호출 / Then: 포맷된 문자열 반환")
        void returnsFormattedString() {
            CodeGroupChangedEvent event = CodeGroupChangedEvent.itemCreated(
                    eventSource, CodeGroupSource.DYNAMIC_DB, "LANG", "KO");

            String result = event.toString();

            assertThat(result).contains("CodeGroupChangedEvent");
            assertThat(result).contains("DYNAMIC_DB");
            assertThat(result).contains("LANG");
            assertThat(result).contains("KO");
            assertThat(result).contains("ITEM_CREATED");
        }
    }
}
