package com.example.admin.permission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.admin.permission.domain.PermissionGroup;
import com.example.admin.permission.domain.PermissionGroupRoot;
import com.example.admin.permission.event.PermissionSetChangedEvent;
import com.example.admin.permission.repository.PermissionGroupRepository;
import com.example.admin.permission.repository.PermissionGroupRootRepository;
import com.example.common.version.ChangeAction;
import com.example.testing.bdd.Scenario;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * PermissionGroupService 테스트.
 *
 * <p>PermissionGroup은 이제 SCD Type 2 버전관리를 사용합니다.
 * PermissionGroupRoot가 권한그룹 코드를 관리하고, PermissionGroup은 버전 정보를 담습니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionGroupService 테스트")
class PermissionGroupServiceTest {

    @Mock
    private PermissionGroupRootRepository rootRepository;

    @Mock
    private PermissionGroupRepository versionRepository;

    private PermissionGroupService service;

    @BeforeEach
    void setUp() {
        service = new PermissionGroupService(rootRepository, versionRepository);
    }

    private PermissionGroupRoot createTestGroupRoot(String code, String name) {
        OffsetDateTime now = OffsetDateTime.now();
        PermissionGroupRoot root = PermissionGroupRoot.createWithCode(code, now);

        // 현재 버전 생성
        PermissionGroup version = PermissionGroup.create(
                root,
                1,
                name,
                "테스트용 그룹",
                true,
                List.of(),
                List.of(),
                ChangeAction.CREATE,
                "테스트 생성",
                "SYSTEM",
                "System",
                now);

        root.activateNewVersion(version, now);
        return root;
    }

    @Test
    @DisplayName("Given 그룹 정보가 저장소에 있을 때 When 반복 조회하면 Then 캐시가 활용된다")
    void givenGroupInRepository_whenFetchingMultipleTimes_thenUsesCache() {
        PermissionGroupRoot root = createTestGroupRoot("AUDITOR", "Auditor");
        given(rootRepository.findByGroupCode("AUDITOR")).willReturn(Optional.of(root));

        Scenario.given("권한 그룹 서비스", () -> service)
                .when("첫 조회", svc -> svc.getByCodeOrThrow("AUDITOR"))
                .then("그룹 반환", loaded -> assertThat(loaded).isEqualTo(root.getCurrentVersion()))
                .and(
                        "두 번째 조회 시 캐시 사용",
                        loaded -> {
                            PermissionGroup second = service.getByCodeOrThrow("AUDITOR");
                            assertThat(second).isEqualTo(root.getCurrentVersion());
                            verify(rootRepository).findByGroupCode("AUDITOR");
                        });
    }

    @Test
    @DisplayName("Given 존재하지 않는 그룹 When 조회하면 Then 예외를 던진다")
    void givenUnknownGroup_whenFetching_thenThrow() {
        given(rootRepository.findByGroupCode("MISSING")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByCodeOrThrow("MISSING"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Given 캐시된 그룹 When evict 호출 후 재조회하면 Then 저장소를 다시 조회한다")
    void givenCache_whenEvicted_thenReloadFromRepository() {
        PermissionGroupRoot root = createTestGroupRoot("ANALYST", "Analyst");
        given(rootRepository.findByGroupCode("ANALYST")).willReturn(Optional.of(root));

        service.getByCodeOrThrow("ANALYST");
        service.evict("ANALYST");
        service.getByCodeOrThrow("ANALYST");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(rootRepository, org.mockito.Mockito.times(2)).findByGroupCode(captor.capture());
        assertThat(captor.getAllValues()).containsOnly("ANALYST");
    }

    @Nested
    @DisplayName("findRootByCode 메서드")
    class FindRootByCodeTest {

        @Test
        @DisplayName("Given 존재하는 그룹 When findRootByCode Then Optional 반환")
        void givenExistingGroup_whenFindRootByCode_thenReturnsOptional() {
            PermissionGroupRoot root = createTestGroupRoot("ADMIN", "Admin");
            given(rootRepository.findByGroupCode("ADMIN")).willReturn(Optional.of(root));

            Optional<PermissionGroupRoot> result = service.findRootByCode("ADMIN");

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(root);
        }

        @Test
        @DisplayName("Given 존재하지 않는 그룹 When findRootByCode Then empty Optional")
        void givenNonExistingGroup_whenFindRootByCode_thenReturnsEmpty() {
            given(rootRepository.findByGroupCode("MISSING")).willReturn(Optional.empty());

            Optional<PermissionGroupRoot> result = service.findRootByCode("MISSING");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getRootByCodeOrThrow 메서드")
    class GetRootByCodeOrThrowTest {

        @Test
        @DisplayName("Given 존재하는 그룹 When getRootByCodeOrThrow Then root 반환")
        void givenExistingGroup_whenGetRootByCodeOrThrow_thenReturnsRoot() {
            PermissionGroupRoot root = createTestGroupRoot("VIEWER", "Viewer");
            given(rootRepository.findByGroupCode("VIEWER")).willReturn(Optional.of(root));

            PermissionGroupRoot result = service.getRootByCodeOrThrow("VIEWER");

            assertThat(result).isEqualTo(root);
        }

        @Test
        @DisplayName("Given 존재하지 않는 그룹 When getRootByCodeOrThrow Then 예외")
        void givenNonExistingGroup_whenGetRootByCodeOrThrow_thenThrows() {
            given(rootRepository.findByGroupCode("MISSING")).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getRootByCodeOrThrow("MISSING"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("권한그룹을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("evictAll 메서드")
    class EvictAllTest {

        @Test
        @DisplayName("Given 캐시된 여러 그룹 When evictAll Then 모든 캐시 무효화")
        void givenMultipleCachedGroups_whenEvictAll_thenClearAll() {
            PermissionGroupRoot root1 = createTestGroupRoot("GROUP1", "Group 1");
            PermissionGroupRoot root2 = createTestGroupRoot("GROUP2", "Group 2");
            given(rootRepository.findByGroupCode("GROUP1")).willReturn(Optional.of(root1));
            given(rootRepository.findByGroupCode("GROUP2")).willReturn(Optional.of(root2));

            // 캐시 등록
            service.getByCodeOrThrow("GROUP1");
            service.getByCodeOrThrow("GROUP2");

            // 모든 캐시 무효화
            service.evictAll();

            // 재조회 시 저장소 다시 호출
            service.getByCodeOrThrow("GROUP1");
            service.getByCodeOrThrow("GROUP2");

            verify(rootRepository, org.mockito.Mockito.times(2)).findByGroupCode("GROUP1");
            verify(rootRepository, org.mockito.Mockito.times(2)).findByGroupCode("GROUP2");
        }
    }

    @Nested
    @DisplayName("publishChange 메서드")
    class PublishChangeTest {

        @Test
        @DisplayName("Given eventPublisher 설정됨 When publishChange Then 이벤트 발행")
        void givenEventPublisher_whenPublishChange_thenPublishesEvent() {
            ApplicationEventPublisher mockPublisher = org.mockito.Mockito.mock(ApplicationEventPublisher.class);
            service.setApplicationEventPublisher(mockPublisher);

            service.publishChange("user123");

            ArgumentCaptor<PermissionSetChangedEvent> captor = ArgumentCaptor.forClass(PermissionSetChangedEvent.class);
            verify(mockPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().principalId()).isEqualTo("user123");
        }

        @Test
        @DisplayName("Given eventPublisher 설정됨 When publishChange with null Then 전체 변경 이벤트 발행")
        void givenEventPublisher_whenPublishChangeWithNull_thenPublishesGlobalEvent() {
            ApplicationEventPublisher mockPublisher = org.mockito.Mockito.mock(ApplicationEventPublisher.class);
            service.setApplicationEventPublisher(mockPublisher);

            service.publishChange(null);

            ArgumentCaptor<PermissionSetChangedEvent> captor = ArgumentCaptor.forClass(PermissionSetChangedEvent.class);
            verify(mockPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().principalId()).isNull();
        }

        @Test
        @DisplayName("Given eventPublisher 미설정 When publishChange Then 아무 일도 일어나지 않음")
        void givenNoEventPublisher_whenPublishChange_thenNoException() {
            // eventPublisher가 null인 상태로 테스트
            PermissionGroupService freshService = new PermissionGroupService(rootRepository, versionRepository);

            // 예외 없이 실행되어야 함
            freshService.publishChange("user123");
        }
    }

    @Nested
    @DisplayName("getByCodeOrThrow - 현재 버전이 null인 경우")
    class GetByCodeOrThrowNoCurrentVersionTest {

        @Test
        @DisplayName("Given root는 있지만 현재 버전이 null When getByCodeOrThrow Then 예외")
        void givenRootWithoutCurrentVersion_whenGetByCodeOrThrow_thenThrows() {
            OffsetDateTime now = OffsetDateTime.now();
            PermissionGroupRoot root = PermissionGroupRoot.createWithCode("EMPTY", now);
            // 현재 버전을 설정하지 않음 (currentVersion = null)
            given(rootRepository.findByGroupCode("EMPTY")).willReturn(Optional.of(root));

            assertThatThrownBy(() -> service.getByCodeOrThrow("EMPTY"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("권한그룹의 활성 버전이 없습니다");
        }
    }
}
