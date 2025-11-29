package com.example.admin.rowaccesspolicy.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.common.security.RowScope;

@DisplayName("RowAccessPolicyRoot 도메인 테스트")
class RowAccessPolicyRootTest {

    @Nested
    @DisplayName("생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("create() - UUID 기반 policyCode로 생성된다")
        void create_generatesUuidPolicyCode() {
            // given
            OffsetDateTime now = OffsetDateTime.now();

            // when
            RowAccessPolicyRoot root = RowAccessPolicyRoot.create(now);

            // then
            assertThat(root.getPolicyCode()).isNotNull();
            assertThat(root.getPolicyCode()).hasSize(36); // UUID format
            assertThat(root.getCreatedAt()).isEqualTo(now);
            assertThat(root.getUpdatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("createWithCode() - 지정된 policyCode로 생성된다")
        void createWithCode_usesProvidedCode() {
            // given
            String policyCode = "CUSTOM_POLICY_001";
            OffsetDateTime now = OffsetDateTime.now();

            // when
            RowAccessPolicyRoot root = RowAccessPolicyRoot.createWithCode(policyCode, now);

            // then
            assertThat(root.getPolicyCode()).isEqualTo(policyCode);
            assertThat(root.getCreatedAt()).isEqualTo(now);
            assertThat(root.getUpdatedAt()).isEqualTo(now);
        }
    }

    @Nested
    @DisplayName("touch() 테스트")
    class TouchTest {

        @Test
        @DisplayName("updatedAt이 갱신된다")
        void touch_updatesUpdatedAt() {
            // given
            OffsetDateTime createdAt = OffsetDateTime.now().minusDays(1);
            RowAccessPolicyRoot root = RowAccessPolicyRoot.create(createdAt);
            OffsetDateTime newTime = OffsetDateTime.now();

            // when
            root.touch(newTime);

            // then
            assertThat(root.getUpdatedAt()).isEqualTo(newTime);
            assertThat(root.getCreatedAt()).isEqualTo(createdAt);
        }
    }

    @Nested
    @DisplayName("버전 관리 테스트")
    class VersionManagementTest {

        @Test
        @DisplayName("activateNewVersion() - 새 버전을 활성화하고 현재 버전을 이전 버전으로 이동")
        void activateNewVersion_movesCurrentToPrevious() {
            // given
            OffsetDateTime now = OffsetDateTime.now();
            RowAccessPolicyRoot root = RowAccessPolicyRoot.create(now);

            RowAccessPolicy v1 = mock(RowAccessPolicy.class);
            RowAccessPolicy v2 = mock(RowAccessPolicy.class);

            root.activateNewVersion(v1, now);
            OffsetDateTime later = now.plusMinutes(10);

            // when
            root.activateNewVersion(v2, later);

            // then
            assertThat(root.getCurrentVersion()).isEqualTo(v2);
            assertThat(root.getPreviousVersion()).isEqualTo(v1);
            assertThat(root.getNextVersion()).isNull();
            assertThat(root.getUpdatedAt()).isEqualTo(later);
        }

        @Test
        @DisplayName("setDraftVersion() - 초안 버전을 nextVersion에 설정")
        void setDraftVersion_setsNextVersion() {
            // given
            OffsetDateTime now = OffsetDateTime.now();
            RowAccessPolicyRoot root = RowAccessPolicyRoot.create(now);
            RowAccessPolicy draft = mock(RowAccessPolicy.class);

            // when
            root.setDraftVersion(draft);

            // then
            assertThat(root.getNextVersion()).isEqualTo(draft);
        }

        @Test
        @DisplayName("discardDraft() - 초안을 삭제하면 nextVersion이 null이 된다")
        void discardDraft_clearsNextVersion() {
            // given
            OffsetDateTime now = OffsetDateTime.now();
            RowAccessPolicyRoot root = RowAccessPolicyRoot.create(now);
            RowAccessPolicy draft = mock(RowAccessPolicy.class);
            root.setDraftVersion(draft);

            // when
            root.discardDraft();

            // then
            assertThat(root.getNextVersion()).isNull();
        }
    }

    @Nested
    @DisplayName("publishDraft() 테스트")
    class PublishDraftTest {

        @Test
        @DisplayName("초안이 없으면 예외 발생")
        void publishDraft_noDraft_throwsException() {
            // given
            OffsetDateTime now = OffsetDateTime.now();
            RowAccessPolicyRoot root = RowAccessPolicyRoot.create(now);

            // when & then
            assertThatThrownBy(() -> root.publishDraft(now))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("게시할 초안 버전이 없습니다.");
        }

        @Test
        @DisplayName("nextVersion이 Draft가 아니면 예외 발생")
        void publishDraft_notDraft_throwsException() {
            // given
            OffsetDateTime now = OffsetDateTime.now();
            RowAccessPolicyRoot root = RowAccessPolicyRoot.create(now);

            RowAccessPolicy notDraft = mock(RowAccessPolicy.class);
            when(notDraft.isDraft()).thenReturn(false);
            root.setDraftVersion(notDraft);

            // when & then
            assertThatThrownBy(() -> root.publishDraft(now))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("게시할 초안 버전이 없습니다.");
        }

        @Test
        @DisplayName("초안 게시 - 현재 버전이 없는 경우")
        void publishDraft_noCurrentVersion_publishesDraft() {
            // given
            OffsetDateTime now = OffsetDateTime.now();
            RowAccessPolicyRoot root = RowAccessPolicyRoot.create(now);

            RowAccessPolicy draft = mock(RowAccessPolicy.class);
            when(draft.isDraft()).thenReturn(true);
            root.setDraftVersion(draft);

            OffsetDateTime publishTime = now.plusMinutes(10);

            // when
            root.publishDraft(publishTime);

            // then
            verify(draft).publish(publishTime);
            assertThat(root.getCurrentVersion()).isEqualTo(draft);
            assertThat(root.getPreviousVersion()).isNull();
            assertThat(root.getNextVersion()).isNull();
            assertThat(root.getUpdatedAt()).isEqualTo(publishTime);
        }

        @Test
        @DisplayName("초안 게시 - 현재 버전이 있는 경우 종료 후 게시")
        void publishDraft_hasCurrentVersion_closesAndPublishes() {
            // given
            OffsetDateTime now = OffsetDateTime.now();
            RowAccessPolicyRoot root = RowAccessPolicyRoot.create(now);

            RowAccessPolicy current = mock(RowAccessPolicy.class);
            root.activateNewVersion(current, now);

            RowAccessPolicy draft = mock(RowAccessPolicy.class);
            when(draft.isDraft()).thenReturn(true);
            root.setDraftVersion(draft);

            OffsetDateTime publishTime = now.plusMinutes(10);

            // when
            root.publishDraft(publishTime);

            // then
            verify(current).close(publishTime);
            verify(draft).publish(publishTime);
            assertThat(root.getCurrentVersion()).isEqualTo(draft);
            assertThat(root.getPreviousVersion()).isEqualTo(current);
            assertThat(root.getNextVersion()).isNull();
        }
    }

    @Nested
    @DisplayName("canRollback() 테스트")
    class CanRollbackTest {

        @Test
        @DisplayName("이전 버전이 없으면 false")
        void canRollback_noPreviousVersion_returnsFalse() {
            // given
            OffsetDateTime now = OffsetDateTime.now();
            RowAccessPolicyRoot root = RowAccessPolicyRoot.create(now);

            // when & then
            assertThat(root.canRollback()).isFalse();
        }

        @Test
        @DisplayName("이전 버전이 있으면 true")
        void canRollback_hasPreviousVersion_returnsTrue() {
            // given
            OffsetDateTime now = OffsetDateTime.now();
            RowAccessPolicyRoot root = RowAccessPolicyRoot.create(now);

            RowAccessPolicy v1 = mock(RowAccessPolicy.class);
            RowAccessPolicy v2 = mock(RowAccessPolicy.class);
            root.activateNewVersion(v1, now);
            root.activateNewVersion(v2, now.plusMinutes(10));

            // when & then
            assertThat(root.canRollback()).isTrue();
        }
    }

    @Nested
    @DisplayName("getCurrentVersionNumber() 테스트")
    class GetCurrentVersionNumberTest {

        @Test
        @DisplayName("현재 버전이 없으면 null")
        void getCurrentVersionNumber_noCurrentVersion_returnsNull() {
            // given
            OffsetDateTime now = OffsetDateTime.now();
            RowAccessPolicyRoot root = RowAccessPolicyRoot.create(now);

            // when & then
            assertThat(root.getCurrentVersionNumber()).isNull();
        }

        @Test
        @DisplayName("현재 버전이 있으면 버전 번호 반환")
        void getCurrentVersionNumber_hasCurrentVersion_returnsVersion() {
            // given
            OffsetDateTime now = OffsetDateTime.now();
            RowAccessPolicyRoot root = RowAccessPolicyRoot.create(now);

            RowAccessPolicy current = mock(RowAccessPolicy.class);
            when(current.getVersion()).thenReturn(3);
            root.activateNewVersion(current, now);

            // when & then
            assertThat(root.getCurrentVersionNumber()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("hasDraft() 테스트")
    class HasDraftTest {

        @Test
        @DisplayName("nextVersion이 없으면 false")
        void hasDraft_noNextVersion_returnsFalse() {
            // given
            OffsetDateTime now = OffsetDateTime.now();
            RowAccessPolicyRoot root = RowAccessPolicyRoot.create(now);

            // when & then
            assertThat(root.hasDraft()).isFalse();
        }

        @Test
        @DisplayName("nextVersion이 Draft가 아니면 false")
        void hasDraft_nextVersionNotDraft_returnsFalse() {
            // given
            OffsetDateTime now = OffsetDateTime.now();
            RowAccessPolicyRoot root = RowAccessPolicyRoot.create(now);

            RowAccessPolicy notDraft = mock(RowAccessPolicy.class);
            when(notDraft.isDraft()).thenReturn(false);
            root.setDraftVersion(notDraft);

            // when & then
            assertThat(root.hasDraft()).isFalse();
        }

        @Test
        @DisplayName("nextVersion이 Draft이면 true")
        void hasDraft_nextVersionIsDraft_returnsTrue() {
            // given
            OffsetDateTime now = OffsetDateTime.now();
            RowAccessPolicyRoot root = RowAccessPolicyRoot.create(now);

            RowAccessPolicy draft = mock(RowAccessPolicy.class);
            when(draft.isDraft()).thenReturn(true);
            root.setDraftVersion(draft);

            // when & then
            assertThat(root.hasDraft()).isTrue();
        }
    }

    @Nested
    @DisplayName("편의 메서드 테스트 (현재 버전에서 조회)")
    class ConvenienceMethodsTest {

        @Test
        @DisplayName("getName() - 현재 버전이 없으면 null")
        void getName_noCurrentVersion_returnsNull() {
            // given
            OffsetDateTime now = OffsetDateTime.now();
            RowAccessPolicyRoot root = RowAccessPolicyRoot.create(now);

            // when & then
            assertThat(root.getName()).isNull();
        }

        @Test
        @DisplayName("getName() - 현재 버전의 이름 반환")
        void getName_hasCurrentVersion_returnsName() {
            // given
            OffsetDateTime now = OffsetDateTime.now();
            RowAccessPolicyRoot root = RowAccessPolicyRoot.create(now);

            RowAccessPolicy current = mock(RowAccessPolicy.class);
            when(current.getName()).thenReturn("Test Policy");
            root.activateNewVersion(current, now);

            // when & then
            assertThat(root.getName()).isEqualTo("Test Policy");
        }

        @Test
        @DisplayName("getDescription() - 현재 버전이 없으면 null")
        void getDescription_noCurrentVersion_returnsNull() {
            // given
            OffsetDateTime now = OffsetDateTime.now();
            RowAccessPolicyRoot root = RowAccessPolicyRoot.create(now);

            // when & then
            assertThat(root.getDescription()).isNull();
        }

        @Test
        @DisplayName("getDescription() - 현재 버전의 설명 반환")
        void getDescription_hasCurrentVersion_returnsDescription() {
            // given
            OffsetDateTime now = OffsetDateTime.now();
            RowAccessPolicyRoot root = RowAccessPolicyRoot.create(now);

            RowAccessPolicy current = mock(RowAccessPolicy.class);
            when(current.getDescription()).thenReturn("Test Description");
            root.activateNewVersion(current, now);

            // when & then
            assertThat(root.getDescription()).isEqualTo("Test Description");
        }

        @Test
        @DisplayName("isActive() - 현재 버전이 없으면 false")
        void isActive_noCurrentVersion_returnsFalse() {
            // given
            OffsetDateTime now = OffsetDateTime.now();
            RowAccessPolicyRoot root = RowAccessPolicyRoot.create(now);

            // when & then
            assertThat(root.isActive()).isFalse();
        }

        @Test
        @DisplayName("isActive() - 현재 버전이 비활성이면 false")
        void isActive_currentVersionInactive_returnsFalse() {
            // given
            OffsetDateTime now = OffsetDateTime.now();
            RowAccessPolicyRoot root = RowAccessPolicyRoot.create(now);

            RowAccessPolicy current = mock(RowAccessPolicy.class);
            when(current.isActive()).thenReturn(false);
            root.activateNewVersion(current, now);

            // when & then
            assertThat(root.isActive()).isFalse();
        }

        @Test
        @DisplayName("isActive() - 현재 버전이 활성이면 true")
        void isActive_currentVersionActive_returnsTrue() {
            // given
            OffsetDateTime now = OffsetDateTime.now();
            RowAccessPolicyRoot root = RowAccessPolicyRoot.create(now);

            RowAccessPolicy current = mock(RowAccessPolicy.class);
            when(current.isActive()).thenReturn(true);
            root.activateNewVersion(current, now);

            // when & then
            assertThat(root.isActive()).isTrue();
        }

        @Test
        @DisplayName("getPriority() - 현재 버전이 없으면 null")
        void getPriority_noCurrentVersion_returnsNull() {
            // given
            OffsetDateTime now = OffsetDateTime.now();
            RowAccessPolicyRoot root = RowAccessPolicyRoot.create(now);

            // when & then
            assertThat(root.getPriority()).isNull();
        }

        @Test
        @DisplayName("getPriority() - 현재 버전의 우선순위 반환")
        void getPriority_hasCurrentVersion_returnsPriority() {
            // given
            OffsetDateTime now = OffsetDateTime.now();
            RowAccessPolicyRoot root = RowAccessPolicyRoot.create(now);

            RowAccessPolicy current = mock(RowAccessPolicy.class);
            when(current.getPriority()).thenReturn(100);
            root.activateNewVersion(current, now);

            // when & then
            assertThat(root.getPriority()).isEqualTo(100);
        }

        @Test
        @DisplayName("getRowScope() - 현재 버전이 없으면 null")
        void getRowScope_noCurrentVersion_returnsNull() {
            // given
            OffsetDateTime now = OffsetDateTime.now();
            RowAccessPolicyRoot root = RowAccessPolicyRoot.create(now);

            // when & then
            assertThat(root.getRowScope()).isNull();
        }

        @Test
        @DisplayName("getRowScope() - 현재 버전의 RowScope 반환")
        void getRowScope_hasCurrentVersion_returnsRowScope() {
            // given
            OffsetDateTime now = OffsetDateTime.now();
            RowAccessPolicyRoot root = RowAccessPolicyRoot.create(now);

            RowAccessPolicy current = mock(RowAccessPolicy.class);
            when(current.getRowScope()).thenReturn(RowScope.ORG);
            root.activateNewVersion(current, now);

            // when & then
            assertThat(root.getRowScope()).isEqualTo(RowScope.ORG);
        }
    }
}
