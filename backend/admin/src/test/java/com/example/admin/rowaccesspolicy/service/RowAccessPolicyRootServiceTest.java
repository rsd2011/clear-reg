package com.example.admin.rowaccesspolicy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.context.AuthContext;
import com.example.common.security.FeatureCode;
import com.example.admin.rowaccesspolicy.domain.RowAccessPolicy;
import com.example.admin.rowaccesspolicy.domain.RowAccessPolicyRoot;
import com.example.admin.rowaccesspolicy.dto.RowAccessPolicyHistoryResponse;
import com.example.admin.rowaccesspolicy.dto.RowAccessPolicyRootRequest;
import com.example.admin.rowaccesspolicy.dto.RowAccessPolicyRootResponse;
import com.example.admin.rowaccesspolicy.exception.RowAccessPolicyRootNotFoundException;
import com.example.admin.rowaccesspolicy.repository.RowAccessPolicyRootRepository;
import com.example.common.security.RowScope;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;

@DisplayName("RowAccessPolicyRootService")
class RowAccessPolicyRootServiceTest {

    private RowAccessPolicyRootRepository rootRepository;
    private RowAccessPolicyVersioningService versionService;
    private RowAccessPolicyRootService service;

    @BeforeEach
    void setUp() {
        rootRepository = mock(RowAccessPolicyRootRepository.class);
        versionService = mock(RowAccessPolicyVersioningService.class);
        service = new RowAccessPolicyRootService(rootRepository, versionService);
    }

    private AuthContext testContext() {
        return AuthContext.of("testuser", "ORG1", null, null, null, List.of());
    }

    private RowAccessPolicyRoot createTestPolicyWithVersion(String name, int priority, boolean active) {
        OffsetDateTime now = OffsetDateTime.now();
        RowAccessPolicyRoot root = RowAccessPolicyRoot.create(now);
        RowAccessPolicy version = RowAccessPolicy.create(
                root, 1, name, "설명",
                FeatureCode.DRAFT, null, null, null,
                RowScope.OWN, priority, active,
                null, null,
                ChangeAction.CREATE, null, "user", "사용자", now);
        root.activateNewVersion(version, now);
        return root;
    }

    private RowAccessPolicyRoot createTestPolicyWithVersion(String name) {
        return createTestPolicyWithVersion(name, 100, true);
    }

    private RowAccessPolicyRoot createInactivePolicyWithVersion(String name) {
        return createTestPolicyWithVersion(name, 100, false);
    }

    @Nested
    @DisplayName("list")
    class ListPolicies {

        @Test
        @DisplayName("Given: 정책 목록이 존재할 때 / When: list 호출 / Then: 모든 활성 정책 반환")
        void listReturnsAllActivePolicies() {
            RowAccessPolicyRoot p1 = createTestPolicyWithVersion("정책1");
            RowAccessPolicyRoot p2 = createTestPolicyWithVersion("정책2");
            given(rootRepository.findAll()).willReturn(List.of(p1, p2));

            List<RowAccessPolicyRootResponse> result = service.list(null, true);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(RowAccessPolicyRootResponse::name)
                    .containsExactlyInAnyOrder("정책1", "정책2");
        }

        @Test
        @DisplayName("Given: 키워드가 주어졌을 때 / When: list 호출 / Then: 키워드로 필터링된 결과 반환")
        void listWithKeywordFiltersResults() {
            RowAccessPolicyRoot p1 = createTestPolicyWithVersion("기본 행접근");
            RowAccessPolicyRoot p2 = createTestPolicyWithVersion("특수 정책");
            given(rootRepository.findAll()).willReturn(List.of(p1, p2));

            List<RowAccessPolicyRootResponse> result = service.list("기본", true);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("기본 행접근");
        }

        @Test
        @DisplayName("Given: 빈 키워드 / When: list 호출 / Then: 모든 정책 반환")
        void listWithBlankKeyword() {
            RowAccessPolicyRoot p1 = createTestPolicyWithVersion("정책1");
            RowAccessPolicyRoot p2 = createTestPolicyWithVersion("정책2");
            given(rootRepository.findAll()).willReturn(List.of(p1, p2));

            List<RowAccessPolicyRootResponse> result = service.list("   ", false);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Given: 우선순위가 다른 정책들 / When: list 호출 / Then: 우선순위순 정렬")
        void listSortsByPriority() {
            RowAccessPolicyRoot high = createTestPolicyWithVersion("고우선순위", 10, true);
            RowAccessPolicyRoot low = createTestPolicyWithVersion("저우선순위", 200, true);
            given(rootRepository.findAll()).willReturn(List.of(low, high));

            List<RowAccessPolicyRootResponse> result = service.list(null, false);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("고우선순위");
            assertThat(result.get(1).name()).isEqualTo("저우선순위");
        }

        @Test
        @DisplayName("Given: 비활성 정책이 포함될 때 / When: activeOnly=false / Then: 모든 정책 반환")
        void listIncludesInactiveWhenRequested() {
            RowAccessPolicyRoot active = createTestPolicyWithVersion("활성");
            RowAccessPolicyRoot inactive = createInactivePolicyWithVersion("비활성");
            given(rootRepository.findAll()).willReturn(List.of(active, inactive));

            List<RowAccessPolicyRootResponse> result = service.list(null, false);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Given: activeOnly=true / When: list 호출 / Then: 활성 정책만 반환")
        void listExcludesInactiveWhenActiveOnly() {
            RowAccessPolicyRoot active = createTestPolicyWithVersion("활성");
            RowAccessPolicyRoot inactive = createInactivePolicyWithVersion("비활성");
            given(rootRepository.findAll()).willReturn(List.of(active, inactive));

            List<RowAccessPolicyRootResponse> result = service.list(null, true);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("활성");
        }

        @Test
        @DisplayName("Given: 현재 버전이 없는 정책 / When: list 호출 / Then: 해당 정책은 제외됨")
        void listExcludesPoliciesWithoutCurrentVersion() {
            RowAccessPolicyRoot withVersion = createTestPolicyWithVersion("버전있음");
            RowAccessPolicyRoot withoutVersion = RowAccessPolicyRoot.create(OffsetDateTime.now());
            given(rootRepository.findAll()).willReturn(List.of(withVersion, withoutVersion));

            List<RowAccessPolicyRootResponse> result = service.list(null, false);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("버전있음");
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("Given: 존재하는 정책 ID / When: getById 호출 / Then: 정책 반환")
        void getByIdReturnsPolicy() {
            UUID id = UUID.randomUUID();
            RowAccessPolicyRoot policy = createTestPolicyWithVersion("테스트 정책");
            given(rootRepository.findById(id)).willReturn(Optional.of(policy));

            RowAccessPolicyRootResponse result = service.getById(id);

            assertThat(result.name()).isEqualTo("테스트 정책");
        }

        @Test
        @DisplayName("Given: 존재하지 않는 정책 ID / When: getById 호출 / Then: RowAccessPolicyRootNotFoundException 발생")
        void getByIdThrowsNotFoundException() {
            UUID id = UUID.randomUUID();
            given(rootRepository.findById(id)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(id))
                    .isInstanceOf(RowAccessPolicyRootNotFoundException.class)
                    .hasMessage("행 접근 정책을 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("getByPolicyCode")
    class GetByPolicyCode {

        @Test
        @DisplayName("Given: 존재하는 정책 코드 / When: getByPolicyCode 호출 / Then: 정책 반환")
        void getByPolicyCodeReturnsPolicy() {
            String policyCode = "POLICY_001";
            RowAccessPolicyRoot policy = createTestPolicyWithVersion("테스트 정책");
            given(rootRepository.findByPolicyCode(policyCode)).willReturn(Optional.of(policy));

            RowAccessPolicyRootResponse result = service.getByPolicyCode(policyCode);

            assertThat(result.name()).isEqualTo("테스트 정책");
        }

        @Test
        @DisplayName("Given: 존재하지 않는 정책 코드 / When: getByPolicyCode 호출 / Then: RowAccessPolicyRootNotFoundException 발생")
        void getByPolicyCodeThrowsNotFoundException() {
            String policyCode = "UNKNOWN";
            given(rootRepository.findByPolicyCode(policyCode)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getByPolicyCode(policyCode))
                    .isInstanceOf(RowAccessPolicyRootNotFoundException.class)
                    .hasMessageContaining("UNKNOWN");
        }
    }

    @Nested
    @DisplayName("create")
    class CreatePolicy {

        @Test
        @DisplayName("Given: 유효한 요청 / When: create 호출 / Then: 정책 생성 및 SCD Type 2 버전 기록")
        void createPolicyAndRecordsVersion() {
            given(rootRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            RowAccessPolicyRootRequest request = new RowAccessPolicyRootRequest(
                    "새 정책", "설명", FeatureCode.DRAFT, null,
                    null, null, RowScope.OWN, 100, true, null, null);

            service.create(request, testContext());

            verify(rootRepository).save(any());
            verify(versionService).createInitialVersion(any(), eq(request), any(), any());
        }
    }

    @Nested
    @DisplayName("createWithCode")
    class CreateWithCode {

        @Test
        @DisplayName("Given: 유효한 정책 코드 / When: createWithCode 호출 / Then: 지정된 코드로 생성")
        void createPolicyWithSpecifiedCode() {
            String policyCode = "CUSTOM_CODE";
            given(rootRepository.existsByPolicyCode(policyCode)).willReturn(false);
            given(rootRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            RowAccessPolicyRootRequest request = new RowAccessPolicyRootRequest(
                    "새 정책", "설명", FeatureCode.DRAFT, null,
                    null, null, RowScope.ALL, 100, true, null, null);

            service.createWithCode(policyCode, request, testContext());

            verify(rootRepository).save(any());
            verify(versionService).createInitialVersion(any(), eq(request), any(), any());
        }

        @Test
        @DisplayName("Given: 이미 존재하는 정책 코드 / When: createWithCode 호출 / Then: IllegalArgumentException 발생")
        void throwsExceptionWhenCodeExists() {
            String policyCode = "EXISTING_CODE";
            given(rootRepository.existsByPolicyCode(policyCode)).willReturn(true);

            RowAccessPolicyRootRequest request = new RowAccessPolicyRootRequest(
                    "새 정책", "설명", FeatureCode.DRAFT, null,
                    null, null, RowScope.OWN, 100, true, null, null);

            assertThatThrownBy(() -> service.createWithCode(policyCode, request, testContext()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이미 존재하는 정책 코드입니다");
        }
    }

    @Nested
    @DisplayName("update")
    class UpdatePolicy {

        @Test
        @DisplayName("Given: 존재하는 정책 / When: update 호출 / Then: SCD Type 2 버전 기록")
        void updatePolicyAndRecordsVersion() {
            UUID id = UUID.randomUUID();
            RowAccessPolicyRoot policy = createTestPolicyWithVersion("기존 정책");
            given(rootRepository.findById(id)).willReturn(Optional.of(policy));

            RowAccessPolicyRootRequest request = new RowAccessPolicyRootRequest(
                    "수정된 정책", "수정된 설명", FeatureCode.DRAFT, null,
                    null, null, RowScope.ALL, 50, true, null, null);

            service.update(id, request, testContext());

            verify(versionService).createUpdateVersion(eq(policy), eq(request), any(), any());
        }
    }

    @Nested
    @DisplayName("delete")
    class DeletePolicy {

        @Test
        @DisplayName("Given: 존재하는 정책 / When: delete 호출 / Then: SCD Type 2 삭제 버전 기록")
        void deleteSoftDeletesAndRecordsVersion() {
            UUID id = UUID.randomUUID();
            RowAccessPolicyRoot policy = createTestPolicyWithVersion("삭제할 정책");
            given(rootRepository.findById(id)).willReturn(Optional.of(policy));

            service.delete(id, testContext());

            verify(versionService).createDeleteVersion(eq(policy), any(), any());
        }
    }

    @Nested
    @DisplayName("activate")
    class ActivatePolicy {

        @Test
        @DisplayName("Given: 비활성 정책 / When: activate 호출 / Then: SCD Type 2 복원 버전 기록")
        void activateRestoresAndRecordsVersion() {
            UUID id = UUID.randomUUID();
            RowAccessPolicyRoot policy = createInactivePolicyWithVersion("비활성 정책");
            given(rootRepository.findById(id)).willReturn(Optional.of(policy));

            service.activate(id, testContext());

            verify(versionService).createRestoreVersion(eq(policy), any(), any());
        }
    }

    @Nested
    @DisplayName("getHistory")
    class GetHistory {

        @Test
        @DisplayName("Given: 이력이 있는 정책 / When: getHistory 호출 / Then: SCD Type 2 버전 이력 반환")
        void getHistoryReturnsScdVersionHistory() {
            UUID policyId = UUID.randomUUID();
            RowAccessPolicyHistoryResponse v1 = new RowAccessPolicyHistoryResponse(
                    UUID.randomUUID(), policyId, 1,
                    OffsetDateTime.now().minusDays(1), OffsetDateTime.now(),
                    "정책", "설명",
                    FeatureCode.DRAFT, null, null, null,
                    RowScope.OWN, 100, true,
                    null, null,
                    VersionStatus.HISTORICAL, ChangeAction.CREATE,
                    null, "user1", "사용자1", OffsetDateTime.now().minusDays(1),
                    null, null);
            RowAccessPolicyHistoryResponse v2 = new RowAccessPolicyHistoryResponse(
                    UUID.randomUUID(), policyId, 2,
                    OffsetDateTime.now(), null,
                    "수정된 정책", "수정 설명",
                    FeatureCode.DRAFT, null, null, null,
                    RowScope.ALL, 50, true,
                    null, null,
                    VersionStatus.PUBLISHED, ChangeAction.UPDATE,
                    "변경 사유", "user1", "사용자1", OffsetDateTime.now(),
                    null, null);
            given(versionService.getVersionHistory(policyId)).willReturn(List.of(v2, v1));

            List<RowAccessPolicyHistoryResponse> result = service.getHistory(policyId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).version()).isEqualTo(2);
            assertThat(result.get(0).changeAction()).isEqualTo(ChangeAction.UPDATE);
            assertThat(result.get(1).version()).isEqualTo(1);
            assertThat(result.get(1).changeAction()).isEqualTo(ChangeAction.CREATE);
        }
    }

    @Nested
    @DisplayName("listActive")
    class ListActive {

        @Test
        @DisplayName("Given: 활성 정책 목록 / When: listActive 호출 / Then: 활성 정책만 반환")
        void listActiveReturnsPolicies() {
            RowAccessPolicyRoot p1 = createTestPolicyWithVersion("정책1");
            RowAccessPolicyRoot p2 = createTestPolicyWithVersion("정책2");
            given(rootRepository.findAllActiveOrderByPriority()).willReturn(List.of(p1, p2));

            List<RowAccessPolicyRootResponse> result = service.listActive();

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("listByFeatureCode")
    class ListByFeatureCode {

        @Test
        @DisplayName("Given: 특정 FeatureCode의 정책 / When: listByFeatureCode 호출 / Then: 해당 정책만 반환")
        void listByFeatureCodeReturnsPolicies() {
            RowAccessPolicyRoot policy = createTestPolicyWithVersion("정책");
            given(rootRepository.findActiveByFeatureCode(FeatureCode.DRAFT)).willReturn(List.of(policy));

            List<RowAccessPolicyRootResponse> result = service.listByFeatureCode(FeatureCode.DRAFT);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("listByRowScope")
    class ListByRowScope {

        @Test
        @DisplayName("Given: 특정 RowScope의 정책 / When: listByRowScope 호출 / Then: 해당 정책만 반환")
        void listByRowScopeReturnsPolicies() {
            RowAccessPolicyRoot policy = createTestPolicyWithVersion("정책");
            given(rootRepository.findActiveByRowScope(RowScope.ALL)).willReturn(List.of(policy));

            List<RowAccessPolicyRootResponse> result = service.listByRowScope(RowScope.ALL);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("listWithDraft")
    class ListWithDraft {

        @Test
        @DisplayName("Given: 초안이 있는 정책 / When: listWithDraft 호출 / Then: 초안 있는 정책 반환")
        void listWithDraftReturnsPolicies() {
            RowAccessPolicyRoot policy = createTestPolicyWithVersion("정책");
            given(rootRepository.findAllWithDraft()).willReturn(List.of(policy));

            List<RowAccessPolicyRootResponse> result = service.listWithDraft();

            assertThat(result).hasSize(1);
        }
    }
}
