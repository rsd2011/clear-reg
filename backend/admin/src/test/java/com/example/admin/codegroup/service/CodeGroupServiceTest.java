package com.example.admin.codegroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.example.admin.codegroup.domain.CodeGroup;
import com.example.admin.codegroup.domain.CodeGroupSource;
import com.example.admin.codegroup.domain.CodeItem;
import com.example.admin.codegroup.event.CodeGroupChangedEvent;
import com.example.admin.codegroup.repository.CodeGroupRepository;
import com.example.admin.codegroup.repository.CodeItemRepository;

class CodeGroupServiceTest {

    private final CodeGroupRepository groupRepository = mock(CodeGroupRepository.class);
    private final CodeItemRepository itemRepository = mock(CodeItemRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final CodeGroupService service = new CodeGroupService(groupRepository, itemRepository, eventPublisher);

    @Nested
    @DisplayName("그룹 CRUD 테스트")
    class GroupCrudTests {

        @Test
        @DisplayName("Given: 새 그룹 정보 / When: createGroup 호출 / Then: 그룹이 생성되고 이벤트가 발행된다")
        void createGroup() {
            // Given
            given(groupRepository.existsBySourceAndGroupCode(CodeGroupSource.DYNAMIC_DB, "CUSTOM_CODE")).willReturn(false);
            given(groupRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // When
            CodeGroup result = service.createGroup("custom_code", "사용자 코드", "설명", "admin");

            // Then
            assertThat(result.getGroupCode()).isEqualTo("CUSTOM_CODE");
            assertThat(result.getGroupName()).isEqualTo("사용자 코드");
            assertThat(result.getSource()).isEqualTo(CodeGroupSource.DYNAMIC_DB);
            verify(eventPublisher).publishEvent(any(CodeGroupChangedEvent.class));
        }

        @Test
        @DisplayName("Given: 이미 존재하는 그룹 코드 / When: createGroup 호출 / Then: 예외가 발생한다")
        void createGroupThrowsWhenDuplicate() {
            // Given
            given(groupRepository.existsBySourceAndGroupCode(CodeGroupSource.DYNAMIC_DB, "CUSTOM_CODE")).willReturn(true);

            // When / Then
            assertThatThrownBy(() -> service.createGroup("CUSTOM_CODE", "이름", "설명", "admin"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이미 존재");
        }

        @Test
        @DisplayName("Given: 기존 그룹 / When: updateGroup 호출 / Then: 정보가 업데이트된다")
        void updateGroup() {
            // Given
            UUID groupId = UUID.randomUUID();
            CodeGroup existing = CodeGroup.createDynamic("CODE", "이름", "설명", "old");
            setId(existing, groupId);
            given(groupRepository.findById(groupId)).willReturn(Optional.of(existing));
            given(groupRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // When
            CodeGroup result = service.updateGroup(groupId, "새 이름", 5, true, "새 설명", "admin");

            // Then
            assertThat(result.getGroupName()).isEqualTo("새 이름");
            assertThat(result.getDisplayOrder()).isEqualTo(5);
            verify(eventPublisher).publishEvent(any(CodeGroupChangedEvent.class));
        }

        @Test
        @DisplayName("Given: 존재하지 않는 그룹 ID / When: updateGroup 호출 / Then: 예외가 발생한다")
        void updateGroupThrowsWhenNotFound() {
            // Given
            UUID groupId = UUID.randomUUID();
            given(groupRepository.findById(groupId)).willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.updateGroup(groupId, "이름", 0, true, null, "admin"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않습니다");
        }

        @Test
        @DisplayName("Given: 동적 그룹 / When: deleteGroup 호출 / Then: 그룹과 아이템이 삭제된다")
        void deleteGroup() {
            // Given
            UUID groupId = UUID.randomUUID();
            CodeGroup group = CodeGroup.createDynamic("CODE", "이름", null, "admin");
            setId(group, groupId);
            given(groupRepository.findById(groupId)).willReturn(Optional.of(group));

            // When
            service.deleteGroup(groupId);

            // Then
            verify(itemRepository).deleteAllByGroupId(groupId);
            verify(groupRepository).delete(group);
            verify(eventPublisher).publishEvent(any(CodeGroupChangedEvent.class));
        }

        @Test
        @DisplayName("Given: STATIC_ENUM 그룹 / When: deleteGroup 호출 / Then: 예외가 발생한다")
        void deleteGroupThrowsForStaticEnum() {
            // Given
            UUID groupId = UUID.randomUUID();
            CodeGroup group = CodeGroup.createStaticOverride("STATUS", "상태", null, "admin");
            setId(group, groupId);
            given(groupRepository.findById(groupId)).willReturn(Optional.of(group));

            // When / Then
            assertThatThrownBy(() -> service.deleteGroup(groupId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("동적 그룹만");
        }
    }

    @Nested
    @DisplayName("아이템 CRUD 테스트")
    class ItemCrudTests {

        @Test
        @DisplayName("Given: 그룹과 아이템 정보 / When: createItem 호출 / Then: 아이템이 생성된다")
        void createItem() {
            // Given
            UUID groupId = UUID.randomUUID();
            CodeGroup group = CodeGroup.createDynamic("LANG", "언어", null, "admin");
            setId(group, groupId);
            given(groupRepository.findBySourceAndGroupCode(CodeGroupSource.DYNAMIC_DB, "LANG"))
                    .willReturn(Optional.of(group));
            given(itemRepository.existsBySourceAndGroupCodeAndItemCode(CodeGroupSource.DYNAMIC_DB, "LANG", "ko"))
                    .willReturn(false);
            given(itemRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // When
            CodeItem result = service.createItem(
                    CodeGroupSource.DYNAMIC_DB, "lang", "ko", "한국어",
                    0, true, "한국어 코드", null, "admin"
            );

            // Then
            assertThat(result.getItemCode()).isEqualTo("ko"); // itemCode는 normalize되지 않음
            assertThat(result.getItemName()).isEqualTo("한국어");
            verify(eventPublisher).publishEvent(any(CodeGroupChangedEvent.class));
        }

        @Test
        @DisplayName("Given: 이미 존재하는 아이템 코드 / When: createItem 호출 / Then: 예외가 발생한다")
        void createItemThrowsWhenDuplicate() {
            // Given
            UUID groupId = UUID.randomUUID();
            CodeGroup group = CodeGroup.createDynamic("LANG", "언어", null, "admin");
            setId(group, groupId);
            given(groupRepository.findBySourceAndGroupCode(CodeGroupSource.DYNAMIC_DB, "LANG"))
                    .willReturn(Optional.of(group));
            given(itemRepository.existsBySourceAndGroupCodeAndItemCode(CodeGroupSource.DYNAMIC_DB, "LANG", "KO"))
                    .willReturn(true);

            // When / Then
            assertThatThrownBy(() -> service.createItem(
                    CodeGroupSource.DYNAMIC_DB, "LANG", "KO", "한국어",
                    0, true, null, null, "admin"
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("이미 존재");
        }

        @Test
        @DisplayName("Given: STATIC_ENUM 그룹 / When: createItem 호출 / Then: 예외가 발생한다 (새 아이템 생성 불가)")
        void createItemThrowsForStaticEnum() {
            // Given
            UUID groupId = UUID.randomUUID();
            CodeGroup group = CodeGroup.createStaticOverride("STATUS", "상태", null, "admin");
            setId(group, groupId);
            given(groupRepository.findBySourceAndGroupCode(CodeGroupSource.STATIC_ENUM, "STATUS"))
                    .willReturn(Optional.of(group));

            // When / Then
            assertThatThrownBy(() -> service.createItem(
                    CodeGroupSource.STATIC_ENUM, "STATUS", "NEW_STATUS", "새 상태",
                    0, true, null, null, "admin"
            )).isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("새 아이템을 생성할 수 없습니다");
        }

        @Test
        @DisplayName("Given: 기존 아이템 / When: updateItem 호출 / Then: 정보가 업데이트된다")
        void updateItem() {
            // Given
            UUID itemId = UUID.randomUUID();
            CodeGroup group = CodeGroup.createDynamic("LANG", "언어", null, "admin");
            CodeItem item = CodeItem.create(group, "KO", "한국어", 0, true, null, null, "admin", null);
            setItemId(item, itemId);
            given(itemRepository.findByIdWithGroup(itemId)).willReturn(Optional.of(item));
            given(itemRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // When
            CodeItem result = service.updateItem(itemId, "한글", 5, false, "설명", null, "modifier");

            // Then
            assertThat(result.getItemName()).isEqualTo("한글");
            assertThat(result.getDisplayOrder()).isEqualTo(5);
            assertThat(result.isActive()).isFalse();
            verify(eventPublisher).publishEvent(any(CodeGroupChangedEvent.class));
        }

        @Test
        @DisplayName("Given: 동적 아이템 / When: deleteItem 호출 / Then: 아이템이 삭제된다")
        void deleteItem() {
            // Given
            UUID itemId = UUID.randomUUID();
            CodeGroup group = CodeGroup.createDynamic("LANG", "언어", null, "admin");
            CodeItem item = CodeItem.create(group, "KO", "한국어", 0, true, null, null, "admin", null);
            setItemId(item, itemId);
            given(itemRepository.findByIdWithGroup(itemId)).willReturn(Optional.of(item));

            // When
            service.deleteItem(itemId);

            // Then
            verify(itemRepository).delete(item);
            verify(eventPublisher).publishEvent(any(CodeGroupChangedEvent.class));
        }
    }

    @Nested
    @DisplayName("마이그레이션 테스트")
    class MigrationTests {

        @Test
        @DisplayName("Given: 기존 그룹에 아이템이 있을 때 / When: migrate 호출 / Then: 그룹 코드가 변경된다")
        void migrate() {
            // Given
            UUID groupId = UUID.randomUUID();
            CodeGroup group = CodeGroup.createDynamic("OLD_CODE", "이름", null, "admin");
            setId(group, groupId);
            group.addItem("V1", "값1", 0, true, null, null, "admin");

            given(groupRepository.findById(groupId))
                    .willReturn(Optional.of(group));
            given(groupRepository.existsBySourceAndGroupCode(CodeGroupSource.DYNAMIC_DB, "NEW_CODE"))
                    .willReturn(false);
            given(groupRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // When
            var result = service.migrate(groupId, "NEW_CODE");

            // Then
            assertThat(result.migratedCount()).isEqualTo(1);
            assertThat(result.groupId()).isEqualTo(groupId);
            assertThat(result.oldGroupCode()).isEqualTo("OLD_CODE");
            assertThat(result.newGroupCode()).isEqualTo("NEW_CODE");
            assertThat(group.getGroupCode()).isEqualTo("NEW_CODE");
            verify(eventPublisher).publishEvent(any(CodeGroupChangedEvent.class));
        }

        @Test
        @DisplayName("Given: 기존 그룹이 없을 때 / When: migrate 호출 / Then: 예외가 발생한다")
        void migrateThrowsWhenOldGroupNotFound() {
            // Given
            UUID groupId = UUID.randomUUID();
            given(groupRepository.findById(groupId))
                    .willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.migrate(groupId, "NEW_CODE"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("그룹을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("Given: 새 그룹 코드가 이미 존재할 때 / When: migrate 호출 / Then: 예외가 발생한다")
        void migrateThrowsWhenNewCodeExists() {
            // Given
            UUID groupId = UUID.randomUUID();
            CodeGroup group = CodeGroup.createDynamic("OLD_CODE", "이름", null, "admin");
            setId(group, groupId);
            given(groupRepository.findById(groupId))
                    .willReturn(Optional.of(group));
            given(groupRepository.existsBySourceAndGroupCode(CodeGroupSource.DYNAMIC_DB, "NEW_CODE"))
                    .willReturn(true);

            // When / Then
            assertThatThrownBy(() -> service.migrate(groupId, "NEW_CODE"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 레코드가 존재");
        }

        @Test
        @DisplayName("Given: DYNAMIC_DB가 아닌 소스일 때 / When: migrate 호출 / Then: 예외가 발생한다")
        void migrateThrowsWhenSourceIsNotDynamicDb() {
            // Given
            UUID groupId = UUID.randomUUID();
            CodeGroup group = CodeGroup.createStaticOverride("STATIC_CODE", "그룹명", null, "admin");
            setId(group, groupId);
            given(groupRepository.findById(groupId))
                    .willReturn(Optional.of(group));

            // When / Then
            assertThatThrownBy(() -> service.migrate(groupId, "NEW_CODE"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("DYNAMIC_DB 소스만 마이그레이션 가능합니다");
        }

        @Test
        @DisplayName("Given: 그룹에 아이템이 있을 때 / When: deleteByGroupCode 호출 / Then: 그룹과 아이템이 삭제된다")
        void deleteByGroupCode() {
            // Given
            UUID groupId = UUID.randomUUID();
            CodeGroup group = CodeGroup.createDynamic("CODE", "이름", null, "admin");
            setId(group, groupId);
            group.addItem("V1", "값1", 0, true, null, null, "admin");
            group.addItem("V2", "값2", 1, true, null, null, "admin");

            given(groupRepository.findBySourceAndGroupCode(CodeGroupSource.DYNAMIC_DB, "CODE"))
                    .willReturn(Optional.of(group));

            // When
            int result = service.deleteByGroupCode("CODE");

            // Then
            assertThat(result).isEqualTo(2);
            verify(itemRepository).deleteAllByGroupId(groupId);
            verify(groupRepository).delete(group);
            verify(eventPublisher).publishEvent(any(CodeGroupChangedEvent.class));
        }

        @Test
        @DisplayName("Given: 그룹이 존재하지 않을 때 / When: deleteByGroupCode 호출 / Then: 0을 반환한다")
        void deleteByGroupCodeReturnsZeroWhenNotFound() {
            // Given
            given(groupRepository.findBySourceAndGroupCode(CodeGroupSource.DYNAMIC_DB, "CODE"))
                    .willReturn(Optional.empty());

            // When
            int result = service.deleteByGroupCode("CODE");

            // Then
            assertThat(result).isEqualTo(0);
            verify(groupRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("오버라이드 테스트")
    class OverrideTests {

        @Test
        @DisplayName("Given: 오버라이드가 없을 때 / When: createOrUpdateOverride 호출 / Then: 새로 생성된다")
        void createOrUpdateOverrideCreatesNew() {
            // Given
            given(groupRepository.findBySourceAndGroupCode(CodeGroupSource.STATIC_ENUM, "STATUS"))
                    .willReturn(Optional.empty());
            given(groupRepository.save(any())).willAnswer(inv -> {
                CodeGroup saved = inv.getArgument(0);
                setId(saved, UUID.randomUUID());
                return saved;
            });
            given(itemRepository.findBySourceAndGroupCodeAndItemCode(CodeGroupSource.STATIC_ENUM, "STATUS", "ACTIVE"))
                    .willReturn(Optional.empty());
            given(itemRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // When
            CodeItem result = service.createOrUpdateOverride(
                    "STATUS", "ACTIVE", "활성화", 0, "설명", null, "admin"
            );

            // Then
            assertThat(result.getItemCode()).isEqualTo("ACTIVE");
            assertThat(result.getItemName()).isEqualTo("활성화");
            verify(eventPublisher).publishEvent(any(CodeGroupChangedEvent.class));
        }

        @Test
        @DisplayName("Given: 오버라이드가 이미 있을 때 / When: createOrUpdateOverride 호출 / Then: 업데이트된다")
        void createOrUpdateOverrideUpdatesExisting() {
            // Given
            UUID groupId = UUID.randomUUID();
            CodeGroup group = CodeGroup.createStaticOverride("STATUS", "상태", null, "admin");
            setId(group, groupId);
            CodeItem existingItem = CodeItem.create(group, "ACTIVE", "활성", 0, true, null, null, "admin", null);
            setItemId(existingItem, UUID.randomUUID());

            given(groupRepository.findBySourceAndGroupCode(CodeGroupSource.STATIC_ENUM, "STATUS"))
                    .willReturn(Optional.of(group));
            given(itemRepository.findBySourceAndGroupCodeAndItemCode(CodeGroupSource.STATIC_ENUM, "STATUS", "ACTIVE"))
                    .willReturn(Optional.of(existingItem));
            given(itemRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // When
            CodeItem result = service.createOrUpdateOverride(
                    "STATUS", "ACTIVE", "활성화됨", 5, "새 설명", null, "modifier"
            );

            // Then
            assertThat(result.getItemName()).isEqualTo("활성화됨");
            assertThat(result.getDisplayOrder()).isEqualTo(5);
        }
    }


    @Nested
    @DisplayName("조회 메서드 테스트")
    class QueryTests {

        @Test
        @DisplayName("Given: 소스와 그룹 코드 / When: findGroup 호출 / Then: 그룹을 반환한다")
        void findGroup() {
            // Given
            CodeGroup group = CodeGroup.createDynamic("LANG", "언어", null, "admin");
            given(groupRepository.findBySourceAndGroupCode(CodeGroupSource.DYNAMIC_DB, "LANG"))
                    .willReturn(Optional.of(group));

            // When
            Optional<CodeGroup> result = service.findGroup(CodeGroupSource.DYNAMIC_DB, "lang");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getGroupCode()).isEqualTo("LANG");
        }

        @Test
        @DisplayName("Given: 소스와 그룹 코드 / When: findGroupWithItems 호출 / Then: 그룹을 반환한다")
        void findGroupWithItems() {
            // Given
            CodeGroup group = CodeGroup.createDynamic("LANG", "언어", null, "admin");
            given(groupRepository.findBySourceAndGroupCodeWithItems(CodeGroupSource.DYNAMIC_DB, "LANG"))
                    .willReturn(Optional.of(group));

            // When
            Optional<CodeGroup> result = service.findGroupWithItems(CodeGroupSource.DYNAMIC_DB, "lang");

            // Then
            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("Given: 그룹 ID / When: findGroupById 호출 / Then: 그룹을 반환한다")
        void findGroupById() {
            // Given
            UUID groupId = UUID.randomUUID();
            CodeGroup group = CodeGroup.createDynamic("LANG", "언어", null, "admin");
            given(groupRepository.findById(groupId)).willReturn(Optional.of(group));

            // When
            Optional<CodeGroup> result = service.findGroupById(groupId);

            // Then
            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("Given: 소스 / When: findGroupsBySource 호출 / Then: 그룹 목록을 반환한다")
        void findGroupsBySource() {
            // Given
            CodeGroup group = CodeGroup.createDynamic("LANG", "언어", null, "admin");
            given(groupRepository.findAllBySourceOrderByDisplayOrderAscGroupCodeAsc(CodeGroupSource.DYNAMIC_DB))
                    .willReturn(java.util.List.of(group));

            // When
            var result = service.findGroupsBySource(CodeGroupSource.DYNAMIC_DB);

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Given: 소스 / When: findGroupsBySourceWithItems 호출 / Then: 그룹 목록을 반환한다")
        void findGroupsBySourceWithItems() {
            // Given
            CodeGroup group = CodeGroup.createDynamic("LANG", "언어", null, "admin");
            given(groupRepository.findAllBySourceWithItems(CodeGroupSource.DYNAMIC_DB))
                    .willReturn(java.util.List.of(group));

            // When
            var result = service.findGroupsBySourceWithItems(CodeGroupSource.DYNAMIC_DB);

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Given: 소스와 그룹 코드 / When: findActiveItems 호출 / Then: 아이템 목록을 반환한다")
        void findActiveItems() {
            // Given
            given(itemRepository.findActiveBySourceAndGroupCode(CodeGroupSource.DYNAMIC_DB, "LANG"))
                    .willReturn(java.util.List.of());

            // When
            var result = service.findActiveItems(CodeGroupSource.DYNAMIC_DB, "lang");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given: 소스와 그룹 코드 / When: findAllItems 호출 / Then: 아이템 목록을 반환한다")
        void findAllItems() {
            // Given
            given(itemRepository.findBySourceAndGroupCode(CodeGroupSource.DYNAMIC_DB, "LANG"))
                    .willReturn(java.util.List.of());

            // When
            var result = service.findAllItems(CodeGroupSource.DYNAMIC_DB, "lang");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given: 소스, 그룹 코드, 아이템 코드 / When: findItem 호출 / Then: 아이템을 반환한다")
        void findItem() {
            // Given
            CodeGroup group = CodeGroup.createDynamic("LANG", "언어", null, "admin");
            CodeItem item = CodeItem.create(group, "KO", "한국어", 0, true, null, null, "admin", null);
            given(itemRepository.findBySourceAndGroupCodeAndItemCode(CodeGroupSource.DYNAMIC_DB, "LANG", "KO"))
                    .willReturn(Optional.of(item));

            // When
            Optional<CodeItem> result = service.findItem(CodeGroupSource.DYNAMIC_DB, "lang", "KO");

            // Then
            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("Given: 아이템 ID / When: findItemById 호출 / Then: 아이템을 반환한다")
        void findItemById() {
            // Given
            UUID itemId = UUID.randomUUID();
            CodeGroup group = CodeGroup.createDynamic("LANG", "언어", null, "admin");
            CodeItem item = CodeItem.create(group, "KO", "한국어", 0, true, null, null, "admin", null);
            given(itemRepository.findByIdWithGroup(itemId)).willReturn(Optional.of(item));

            // When
            Optional<CodeItem> result = service.findItemById(itemId);

            // Then
            assertThat(result).isPresent();
        }
    }

    @Nested
    @DisplayName("에러 케이스 테스트")
    class ErrorCaseTests {

        @Test
        @DisplayName("Given: null 그룹 코드 / When: createGroup 호출 / Then: 예외가 발생한다")
        void createGroupThrowsForNullCode() {
            // When / Then
            assertThatThrownBy(() -> service.createGroup(null, "이름", null, "admin"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null");
        }

        @Test
        @DisplayName("Given: 존재하지 않는 그룹 / When: deleteGroup 호출 / Then: 예외가 발생한다")
        void deleteGroupThrowsWhenNotFound() {
            // Given
            UUID groupId = UUID.randomUUID();
            given(groupRepository.findById(groupId)).willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.deleteGroup(groupId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않습니다");
        }

        @Test
        @DisplayName("Given: 수정 불가한 그룹 / When: updateGroup 호출 / Then: 예외가 발생한다")
        void updateGroupThrowsWhenNotEditable() {
            // Given - DW 소스는 editable=false
            UUID groupId = UUID.randomUUID();
            CodeGroup group = CodeGroup.create(CodeGroupSource.DW, "EXT_CODE", "외부 코드", null,
                    true, null, 0, "admin", null);
            setId(group, groupId);
            given(groupRepository.findById(groupId)).willReturn(Optional.of(group));

            // When / Then
            assertThatThrownBy(() -> service.updateGroup(groupId, "새 이름", 0, true, null, "admin"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("수정 불가능");
        }

        @Test
        @DisplayName("Given: 존재하지 않는 그룹 / When: createItem 호출 / Then: 예외가 발생한다")
        void createItemThrowsWhenGroupNotFound() {
            // Given
            given(groupRepository.findBySourceAndGroupCode(CodeGroupSource.DYNAMIC_DB, "UNKNOWN"))
                    .willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.createItem(
                    CodeGroupSource.DYNAMIC_DB, "UNKNOWN", "code", "이름",
                    0, true, null, null, "admin"
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("그룹이 존재하지 않습니다");
        }

        @Test
        @DisplayName("Given: 존재하지 않는 아이템 / When: updateItem 호출 / Then: 예외가 발생한다")
        void updateItemThrowsWhenNotFound() {
            // Given
            UUID itemId = UUID.randomUUID();
            given(itemRepository.findByIdWithGroup(itemId)).willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.updateItem(itemId, "이름", 0, true, null, null, "admin"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("아이템이 존재하지 않습니다");
        }

        @Test
        @DisplayName("Given: 수정 불가한 아이템 / When: updateItem 호출 / Then: 예외가 발생한다")
        void updateItemThrowsWhenNotEditable() {
            // Given - DW 소스는 editable=false
            UUID itemId = UUID.randomUUID();
            CodeGroup group = CodeGroup.create(CodeGroupSource.DW, "EXT_CODE", "외부 코드", null,
                    true, null, 0, "admin", null);
            CodeItem item = CodeItem.create(group, "ACTIVE", "활성", 0, true, null, null, "admin", null);
            setItemId(item, itemId);
            given(itemRepository.findByIdWithGroup(itemId)).willReturn(Optional.of(item));

            // When / Then
            assertThatThrownBy(() -> service.updateItem(itemId, "새 이름", 0, true, null, null, "admin"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("수정 불가능");
        }

        @Test
        @DisplayName("Given: 존재하지 않는 아이템 / When: deleteItem 호출 / Then: 예외가 발생한다")
        void deleteItemThrowsWhenNotFound() {
            // Given
            UUID itemId = UUID.randomUUID();
            given(itemRepository.findByIdWithGroup(itemId)).willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.deleteItem(itemId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("아이템이 존재하지 않습니다");
        }

        @Test
        @DisplayName("Given: 삭제 불가한 아이템 / When: deleteItem 호출 / Then: 예외가 발생한다")
        void deleteItemThrowsWhenNotDeletable() {
            // Given - DW 소스는 deletable=false
            UUID itemId = UUID.randomUUID();
            CodeGroup group = CodeGroup.create(CodeGroupSource.DW, "EXT_CODE", "외부 코드", null, true, null, 0, "admin", null);
            CodeItem item = CodeItem.create(group, "EXT1", "외부값1", 0, true, null, null, "admin", null);
            setItemId(item, itemId);
            given(itemRepository.findByIdWithGroup(itemId)).willReturn(Optional.of(item));

            // When / Then
            assertThatThrownBy(() -> service.deleteItem(itemId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("삭제할 수 없는 소스입니다");
        }

        @Test
        @DisplayName("Given: 존재하지 않는 오버라이드 / When: deleteOverride 호출 / Then: 예외가 발생한다")
        void deleteOverrideThrowsWhenNotFound() {
            // Given
            UUID itemId = UUID.randomUUID();
            given(itemRepository.findByIdWithGroup(itemId)).willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.deleteOverride(itemId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않습니다");
        }

        @Test
        @DisplayName("Given: DYNAMIC_DB 아이템 / When: deleteOverride 호출 / Then: deleteItem으로 위임되어 정상 삭제된다")
        void deleteOverrideDelegatesToDeleteItemForDynamicDb() {
            // Given - deleteOverride는 이제 deleteItem으로 위임됨 (deprecated)
            UUID itemId = UUID.randomUUID();
            CodeGroup group = CodeGroup.createDynamic("LANG", "언어", null, "admin");
            CodeItem item = CodeItem.create(group, "KO", "한국어", 0, true, null, null, "admin", null);
            setItemId(item, itemId);
            given(itemRepository.findByIdWithGroup(itemId)).willReturn(Optional.of(item));

            // When
            service.deleteOverride(itemId);

            // Then - DYNAMIC_DB는 삭제 가능하므로 정상 처리됨
            verify(itemRepository).delete(item);
        }

        @Test
        @DisplayName("Given: null 그룹 코드 / When: findGroup 호출 / Then: 예외가 발생한다")
        void findGroupThrowsForNullCode() {
            // When / Then
            assertThatThrownBy(() -> service.findGroup(CodeGroupSource.DYNAMIC_DB, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null");
        }
    }

    @Nested
    @DisplayName("updateItemByGroupAndCode 테스트")
    class UpdateItemByGroupAndCodeTests {

        @Test
        @DisplayName("Given: 기존 아이템 / When: updateItemByGroupAndCode 호출 / Then: 아이템이 업데이트된다")
        void updateItemByGroupAndCode() {
            // Given
            CodeGroup group = CodeGroup.createDynamic("LANG", "언어", null, "admin");
            CodeItem item = CodeItem.create(group, "KO", "한국어", 0, true, null, null, "admin", null);
            setItemId(item, UUID.randomUUID());
            given(itemRepository.findBySourceAndGroupCodeAndItemCode(CodeGroupSource.DYNAMIC_DB, "LANG", "KO"))
                    .willReturn(Optional.of(item));
            given(itemRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // When
            CodeItem result = service.updateItemByGroupAndCode(
                    "lang", "KO", "한글", 5, false, "설명", null, "modifier"
            );

            // Then
            assertThat(result.getItemName()).isEqualTo("한글");
            assertThat(result.getDisplayOrder()).isEqualTo(5);
            assertThat(result.isActive()).isFalse();
            verify(eventPublisher).publishEvent(any(CodeGroupChangedEvent.class));
        }

        @Test
        @DisplayName("Given: 존재하지 않는 아이템 / When: updateItemByGroupAndCode 호출 / Then: 예외가 발생한다")
        void updateItemByGroupAndCodeThrowsWhenNotFound() {
            // Given
            given(itemRepository.findBySourceAndGroupCodeAndItemCode(CodeGroupSource.DYNAMIC_DB, "LANG", "KO"))
                    .willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.updateItemByGroupAndCode(
                    "lang", "KO", "한글", 0, true, null, null, "admin"
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("아이템이 존재하지 않습니다");
        }

        @Test
        @DisplayName("Given: 수정 불가한 아이템 / When: updateItemByGroupAndCode 호출 / Then: 예외가 발생한다")
        void updateItemByGroupAndCodeThrowsWhenNotEditable() {
            // Given - DW 소스는 editable=false
            CodeGroup group = CodeGroup.create(CodeGroupSource.DW, "EXT_CODE", "외부 코드", null, true, null, 0, "admin", null);
            CodeItem item = CodeItem.create(group, "EXT1", "외부값1", 0, true, null, null, "admin", null);
            setItemId(item, UUID.randomUUID());
            given(itemRepository.findBySourceAndGroupCodeAndItemCode(CodeGroupSource.DYNAMIC_DB, "EXT_CODE", "EXT1"))
                    .willReturn(Optional.of(item));

            // When / Then
            assertThatThrownBy(() -> service.updateItemByGroupAndCode(
                    "ext_code", "EXT1", "새 이름", 0, true, null, null, "admin"
            )).isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("수정 불가능");
        }

        @Test
        @DisplayName("Given: null 파라미터 / When: updateItemByGroupAndCode 호출 / Then: 기존 값이 유지된다")
        void updateItemByGroupAndCodeWithNullParams() {
            // Given
            CodeGroup group = CodeGroup.createDynamic("LANG", "언어", null, "admin");
            CodeItem item = CodeItem.create(group, "KO", "한국어", 3, true, "기존 설명", "{}", "admin", null);
            setItemId(item, UUID.randomUUID());
            given(itemRepository.findBySourceAndGroupCodeAndItemCode(CodeGroupSource.DYNAMIC_DB, "LANG", "KO"))
                    .willReturn(Optional.of(item));
            given(itemRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // When
            CodeItem result = service.updateItemByGroupAndCode(
                    "LANG", "KO", null, null, true, null, null, "admin"
            );

            // Then
            assertThat(result.getItemName()).isEqualTo("한국어");
            assertThat(result.getDisplayOrder()).isEqualTo(3);
            assertThat(result.getDescription()).isEqualTo("기존 설명");
            assertThat(result.getMetadataJson()).isEqualTo("{}");
        }
    }

    @Nested
    @DisplayName("오버라이드 상세 테스트")
    class OverrideDetailTests {

        @Test
        @DisplayName("Given: 기존 그룹 / When: getOrCreateStaticOverrideGroup 호출 / Then: 기존 그룹 반환")
        void getOrCreateStaticOverrideGroupReturnsExisting() {
            // Given
            CodeGroup group = CodeGroup.createStaticOverride("STATUS", "상태", null, "admin");
            given(groupRepository.findBySourceAndGroupCode(CodeGroupSource.STATIC_ENUM, "STATUS"))
                    .willReturn(Optional.of(group));

            // When
            CodeGroup result = service.getOrCreateStaticOverrideGroup("status", "상태", "admin");

            // Then
            assertThat(result).isSameAs(group);
            verify(groupRepository, never()).save(any());
        }

        @Test
        @DisplayName("Given: null 파라미터들 / When: createOrUpdateOverride 호출 / Then: 기존 값이 유지된다")
        void createOrUpdateOverrideWithNullParamsKeepsExisting() {
            // Given
            UUID groupId = UUID.randomUUID();
            CodeGroup group = CodeGroup.createStaticOverride("STATUS", "상태", null, "admin");
            setId(group, groupId);
            CodeItem existingItem = CodeItem.create(group, "ACTIVE", "활성", 5, true, "기존 설명", "{}", "admin", null);
            setItemId(existingItem, UUID.randomUUID());

            given(groupRepository.findBySourceAndGroupCode(CodeGroupSource.STATIC_ENUM, "STATUS"))
                    .willReturn(Optional.of(group));
            given(itemRepository.findBySourceAndGroupCodeAndItemCode(CodeGroupSource.STATIC_ENUM, "STATUS", "ACTIVE"))
                    .willReturn(Optional.of(existingItem));
            given(itemRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // When
            CodeItem result = service.createOrUpdateOverride(
                    "STATUS", "ACTIVE", null, null, null, null, "modifier"
            );

            // Then
            assertThat(result.getItemName()).isEqualTo("활성");
            assertThat(result.getDisplayOrder()).isEqualTo(5);
            assertThat(result.getDescription()).isEqualTo("기존 설명");
            assertThat(result.getMetadataJson()).isEqualTo("{}");
        }

        @Test
        @DisplayName("Given: STATIC_ENUM 아이템 / When: deleteOverride 호출 / Then: 아이템이 삭제된다")
        void deleteOverrideSuccess() {
            // Given
            UUID itemId = UUID.randomUUID();
            CodeGroup group = CodeGroup.createStaticOverride("STATUS", "상태", null, "admin");
            CodeItem item = CodeItem.create(group, "ACTIVE", "활성", 0, true, null, null, "admin", null);
            setItemId(item, itemId);
            given(itemRepository.findByIdWithGroup(itemId)).willReturn(Optional.of(item));

            // When
            service.deleteOverride(itemId);

            // Then
            verify(itemRepository).delete(item);
            verify(eventPublisher).publishEvent(any(CodeGroupChangedEvent.class));
        }
    }

    @Nested
    @DisplayName("유틸리티 테스트")
    class UtilityTests {

        @Test
        @DisplayName("Given: evictAll 호출 / Then: 캐시가 무효화된다")
        void evictAll() {
            // When
            service.evictAll();

            // Then - 메서드가 예외 없이 완료되면 성공
        }
    }

    private void setId(CodeGroup group, UUID id) {
        try {
            var field = group.getClass().getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(group, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setItemId(CodeItem item, UUID id) {
        try {
            var field = item.getClass().getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(item, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
