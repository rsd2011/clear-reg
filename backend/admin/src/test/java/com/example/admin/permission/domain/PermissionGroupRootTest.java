package com.example.admin.permission.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PermissionGroupRoot 테스트")
class PermissionGroupRootTest {

    private final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    @Nested
    @DisplayName("create 메서드")
    class CreateTests {

        @Test
        @DisplayName("Given 현재 시간 When create 호출 Then UUID 기반 groupCode로 생성된다")
        void createWithUuidGroupCode() {
            PermissionGroupRoot root = PermissionGroupRoot.create(now);

            assertThat(root.getGroupCode()).isNotBlank();
            assertThat(root.getCreatedAt()).isEqualTo(now);
            assertThat(root.getUpdatedAt()).isEqualTo(now);
            assertThat(root.getCurrentVersion()).isNull();
        }

        @Test
        @DisplayName("Given groupCode와 시간 When createWithCode 호출 Then 지정된 코드로 생성된다")
        void createWithSpecificCode() {
            String groupCode = "TEST_GROUP";
            PermissionGroupRoot root = PermissionGroupRoot.createWithCode(groupCode, now);

            assertThat(root.getGroupCode()).isEqualTo(groupCode);
            assertThat(root.getCreatedAt()).isEqualTo(now);
        }
    }

    @Nested
    @DisplayName("touch 메서드")
    class TouchTests {

        @Test
        @DisplayName("Given 생성된 Root When touch 호출 Then updatedAt이 갱신된다")
        void touchUpdatesTimestamp() {
            PermissionGroupRoot root = PermissionGroupRoot.create(now);
            OffsetDateTime later = now.plusHours(1);

            root.touch(later);

            assertThat(root.getUpdatedAt()).isEqualTo(later);
            assertThat(root.getCreatedAt()).isEqualTo(now);
        }
    }

    @Nested
    @DisplayName("버전 관리")
    class VersionManagementTests {

        @Test
        @DisplayName("Given 현재 버전이 있을 때 When activateNewVersion 호출 Then 현재 버전이 이전 버전으로 이동한다")
        void activateNewVersionMovesCurrent() {
            PermissionGroupRoot root = PermissionGroupRoot.create(now);
            PermissionGroup oldVersion = mock(PermissionGroup.class);
            PermissionGroup newVersion = mock(PermissionGroup.class);

            // 직접 currentVersion을 설정하기 위해 activateNewVersion 먼저 호출
            root.activateNewVersion(oldVersion, now);
            assertThat(root.getCurrentVersion()).isEqualTo(oldVersion);

            OffsetDateTime later = now.plusHours(1);
            root.activateNewVersion(newVersion, later);

            assertThat(root.getCurrentVersion()).isEqualTo(newVersion);
            assertThat(root.getPreviousVersion()).isEqualTo(oldVersion);
            assertThat(root.getNextVersion()).isNull();
            assertThat(root.getUpdatedAt()).isEqualTo(later);
        }

        @Test
        @DisplayName("Given 초안 버전 When setDraftVersion 호출 Then nextVersion에 설정된다")
        void setDraftVersionSetsNextVersion() {
            PermissionGroupRoot root = PermissionGroupRoot.create(now);
            PermissionGroup draft = mock(PermissionGroup.class);

            root.setDraftVersion(draft);

            assertThat(root.getNextVersion()).isEqualTo(draft);
        }

        @Test
        @DisplayName("Given 초안이 있을 때 When discardDraft 호출 Then nextVersion이 null이 된다")
        void discardDraftClearsNextVersion() {
            PermissionGroupRoot root = PermissionGroupRoot.create(now);
            PermissionGroup draft = mock(PermissionGroup.class);
            root.setDraftVersion(draft);

            root.discardDraft();

            assertThat(root.getNextVersion()).isNull();
        }

        @Test
        @DisplayName("Given 초안이 있을 때 When publishDraft 호출 Then 초안이 현재 버전이 된다")
        void publishDraftActivatesDraft() {
            PermissionGroupRoot root = PermissionGroupRoot.create(now);
            PermissionGroup currentVersion = mock(PermissionGroup.class);
            PermissionGroup draft = mock(PermissionGroup.class);
            given(draft.isDraft()).willReturn(true);

            root.activateNewVersion(currentVersion, now);
            root.setDraftVersion(draft);

            OffsetDateTime publishTime = now.plusHours(1);
            root.publishDraft(publishTime);

            verify(currentVersion).close(publishTime);
            verify(draft).publish(publishTime);
            assertThat(root.getCurrentVersion()).isEqualTo(draft);
            assertThat(root.getPreviousVersion()).isEqualTo(currentVersion);
            assertThat(root.getNextVersion()).isNull();
        }

        @Test
        @DisplayName("Given 초안이 없을 때 When publishDraft 호출 Then IllegalStateException 발생")
        void publishDraftWithoutDraftThrows() {
            PermissionGroupRoot root = PermissionGroupRoot.create(now);

            assertThatThrownBy(() -> root.publishDraft(now))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("초안 버전이 없습니다");
        }

        @Test
        @DisplayName("Given nextVersion이 초안이 아닐 때 When publishDraft 호출 Then IllegalStateException 발생")
        void publishDraftWithNonDraftThrows() {
            PermissionGroupRoot root = PermissionGroupRoot.create(now);
            PermissionGroup nonDraft = mock(PermissionGroup.class);
            given(nonDraft.isDraft()).willReturn(false);
            root.setDraftVersion(nonDraft);

            assertThatThrownBy(() -> root.publishDraft(now))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("초안 버전이 없습니다");
        }
    }

    @Nested
    @DisplayName("상태 조회 메서드")
    class StateQueryTests {

        @Test
        @DisplayName("Given 이전 버전이 있을 때 When canRollback 호출 Then true 반환")
        void canRollbackWithPreviousVersion() {
            PermissionGroupRoot root = PermissionGroupRoot.create(now);
            PermissionGroup v1 = mock(PermissionGroup.class);
            PermissionGroup v2 = mock(PermissionGroup.class);
            root.activateNewVersion(v1, now);
            root.activateNewVersion(v2, now.plusHours(1));

            assertThat(root.canRollback()).isTrue();
        }

        @Test
        @DisplayName("Given 이전 버전이 없을 때 When canRollback 호출 Then false 반환")
        void cannotRollbackWithoutPreviousVersion() {
            PermissionGroupRoot root = PermissionGroupRoot.create(now);

            assertThat(root.canRollback()).isFalse();
        }

        @Test
        @DisplayName("Given 현재 버전이 있을 때 When getCurrentVersionNumber 호출 Then 버전 번호 반환")
        void getCurrentVersionNumberWithVersion() {
            PermissionGroupRoot root = PermissionGroupRoot.create(now);
            PermissionGroup version = mock(PermissionGroup.class);
            given(version.getVersion()).willReturn(5);
            root.activateNewVersion(version, now);

            assertThat(root.getCurrentVersionNumber()).isEqualTo(5);
        }

        @Test
        @DisplayName("Given 현재 버전이 없을 때 When getCurrentVersionNumber 호출 Then null 반환")
        void getCurrentVersionNumberWithoutVersion() {
            PermissionGroupRoot root = PermissionGroupRoot.create(now);

            assertThat(root.getCurrentVersionNumber()).isNull();
        }

        @Test
        @DisplayName("Given 초안이 있을 때 When hasDraft 호출 Then true 반환")
        void hasDraftWhenDraftExists() {
            PermissionGroupRoot root = PermissionGroupRoot.create(now);
            PermissionGroup draft = mock(PermissionGroup.class);
            given(draft.isDraft()).willReturn(true);
            root.setDraftVersion(draft);

            assertThat(root.hasDraft()).isTrue();
        }

        @Test
        @DisplayName("Given 초안이 없을 때 When hasDraft 호출 Then false 반환")
        void hasDraftWhenNoDraft() {
            PermissionGroupRoot root = PermissionGroupRoot.create(now);

            assertThat(root.hasDraft()).isFalse();
        }

        @Test
        @DisplayName("Given nextVersion이 초안이 아닐 때 When hasDraft 호출 Then false 반환")
        void hasDraftWhenNextVersionNotDraft() {
            PermissionGroupRoot root = PermissionGroupRoot.create(now);
            PermissionGroup nonDraft = mock(PermissionGroup.class);
            given(nonDraft.isDraft()).willReturn(false);
            root.setDraftVersion(nonDraft);

            assertThat(root.hasDraft()).isFalse();
        }
    }

    @Nested
    @DisplayName("편의 메서드")
    class ConvenienceMethodTests {

        @Test
        @DisplayName("Given 현재 버전이 있을 때 When getName 호출 Then 버전의 이름 반환")
        void getNameWithVersion() {
            PermissionGroupRoot root = PermissionGroupRoot.create(now);
            PermissionGroup version = mock(PermissionGroup.class);
            given(version.getName()).willReturn("Test Group");
            root.activateNewVersion(version, now);

            assertThat(root.getName()).isEqualTo("Test Group");
        }

        @Test
        @DisplayName("Given 현재 버전이 없을 때 When getName 호출 Then null 반환")
        void getNameWithoutVersion() {
            PermissionGroupRoot root = PermissionGroupRoot.create(now);

            assertThat(root.getName()).isNull();
        }

        @Test
        @DisplayName("Given 현재 버전이 있을 때 When getDescription 호출 Then 버전의 설명 반환")
        void getDescriptionWithVersion() {
            PermissionGroupRoot root = PermissionGroupRoot.create(now);
            PermissionGroup version = mock(PermissionGroup.class);
            given(version.getDescription()).willReturn("Test Description");
            root.activateNewVersion(version, now);

            assertThat(root.getDescription()).isEqualTo("Test Description");
        }

        @Test
        @DisplayName("Given 현재 버전이 없을 때 When getDescription 호출 Then null 반환")
        void getDescriptionWithoutVersion() {
            PermissionGroupRoot root = PermissionGroupRoot.create(now);

            assertThat(root.getDescription()).isNull();
        }

        @Test
        @DisplayName("Given 활성 상태인 현재 버전 When isActive 호출 Then true 반환")
        void isActiveWithActiveVersion() {
            PermissionGroupRoot root = PermissionGroupRoot.create(now);
            PermissionGroup version = mock(PermissionGroup.class);
            given(version.isActive()).willReturn(true);
            root.activateNewVersion(version, now);

            assertThat(root.isActive()).isTrue();
        }

        @Test
        @DisplayName("Given 비활성 상태인 현재 버전 When isActive 호출 Then false 반환")
        void isActiveWithInactiveVersion() {
            PermissionGroupRoot root = PermissionGroupRoot.create(now);
            PermissionGroup version = mock(PermissionGroup.class);
            given(version.isActive()).willReturn(false);
            root.activateNewVersion(version, now);

            assertThat(root.isActive()).isFalse();
        }

        @Test
        @DisplayName("Given 현재 버전이 없을 때 When isActive 호출 Then false 반환")
        void isActiveWithoutVersion() {
            PermissionGroupRoot root = PermissionGroupRoot.create(now);

            assertThat(root.isActive()).isFalse();
        }
    }
}
