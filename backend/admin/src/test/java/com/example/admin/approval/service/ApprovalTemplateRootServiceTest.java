package com.example.admin.approval.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.admin.approval.domain.ApprovalGroup;
import com.example.admin.approval.domain.ApprovalTemplate;
import com.example.admin.approval.domain.ApprovalTemplateRoot;
import com.example.admin.approval.domain.ApprovalTemplateStep;
import com.example.admin.approval.exception.ApprovalTemplateRootNotFoundException;
import com.example.admin.approval.repository.ApprovalTemplateRootRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.admin.approval.dto.ApprovalTemplateRootRequest;
import com.example.admin.approval.dto.ApprovalTemplateRootResponse;
import com.example.admin.approval.dto.ApprovalTemplateStepRequest;
import com.example.admin.approval.dto.DisplayOrderUpdateRequest;
import com.example.admin.approval.dto.VersionHistoryResponse;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;
import com.example.admin.permission.context.AuthContext;
import com.example.common.security.RowScope;

class ApprovalTemplateRootServiceTest {

    private ApprovalTemplateRootRepository templateRepo;
    private ApprovalTemplateService versionService;
    private ApprovalTemplateRootService service;

    @BeforeEach
    void setUp() {
        templateRepo = mock(ApprovalTemplateRootRepository.class);
        versionService = mock(ApprovalTemplateService.class);
        service = new ApprovalTemplateRootService(templateRepo, versionService);
    }

    private AuthContext testContext() {
        return AuthContext.of("testuser", "ORG1", null, null, null, RowScope.ORG);
    }

    /**
     * 테스트용 템플릿 생성 (Root + 버전 포함).
     */
    private ApprovalTemplateRoot createTestTemplateWithVersion(String name) {
        OffsetDateTime now = OffsetDateTime.now();
        ApprovalTemplateRoot root = ApprovalTemplateRoot.create(now);
        ApprovalTemplate version = ApprovalTemplate.create(
                root, 1, name, 0, "설명", true,
                ChangeAction.CREATE, null, "user", "사용자", now);
        root.activateNewVersion(version, now);
        return root;
    }

    /**
     * 테스트용 비활성 템플릿 생성.
     */
    private ApprovalTemplateRoot createInactiveTemplateWithVersion(String name) {
        OffsetDateTime now = OffsetDateTime.now();
        ApprovalTemplateRoot root = ApprovalTemplateRoot.create(now);
        ApprovalTemplate version = ApprovalTemplate.create(
                root, 1, name, 0, "설명", false,  // active = false
                ChangeAction.CREATE, null, "user", "사용자", now);
        root.activateNewVersion(version, now);
        return root;
    }

    private ApprovalGroup createTestGroup(String code, String name) {
        return ApprovalGroup.create(code, name, "설명", 0, OffsetDateTime.now());
    }

    /**
     * 테스트용 템플릿 생성 (커스텀 설명 포함).
     */
    private ApprovalTemplateRoot createTestTemplateWithVersionAndDescription(String name, String description) {
        OffsetDateTime now = OffsetDateTime.now();
        ApprovalTemplateRoot root = ApprovalTemplateRoot.create(now);
        ApprovalTemplate version = ApprovalTemplate.create(
                root, 1, name, 0, description, true,
                ChangeAction.CREATE, null, "user", "사용자", now);
        root.activateNewVersion(version, now);
        return root;
    }

    /**
     * 테스트용 템플릿 생성 (커스텀 displayOrder 포함).
     */
    private ApprovalTemplateRoot createTestTemplateWithVersionAndOrder(String name, int displayOrder) {
        OffsetDateTime now = OffsetDateTime.now();
        ApprovalTemplateRoot root = ApprovalTemplateRoot.create(now);
        ApprovalTemplate version = ApprovalTemplate.create(
                root, 1, name, displayOrder, "설명", true,
                ChangeAction.CREATE, null, "user", "사용자", now);
        root.activateNewVersion(version, now);
        return root;
    }

    @Nested
    @DisplayName("list")
    class ListTemplates {

        @Test
        @DisplayName("Given: 템플릿 목록이 존재할 때 / When: list 호출 / Then: 모든 활성 템플릿 반환")
        void listReturnsAllActiveTemplates() {
            ApprovalTemplateRoot t1 = createTestTemplateWithVersion("템플릿1");
            ApprovalTemplateRoot t2 = createTestTemplateWithVersion("템플릿2");
            given(templateRepo.findAll()).willReturn(List.of(t1, t2));

            List<ApprovalTemplateRootResponse> result = service.list(null, true);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(ApprovalTemplateRootResponse::name)
                    .containsExactlyInAnyOrder("템플릿1", "템플릿2");
        }

        @Test
        @DisplayName("Given: 키워드가 주어졌을 때 / When: list 호출 / Then: 키워드로 필터링된 결과 반환")
        void listWithKeywordFiltersResults() {
            ApprovalTemplateRoot t1 = createTestTemplateWithVersion("기본 승인선");
            ApprovalTemplateRoot t2 = createTestTemplateWithVersion("특수 템플릿");
            given(templateRepo.findAll()).willReturn(List.of(t1, t2));

            List<ApprovalTemplateRootResponse> result = service.list("기본", true);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("기본 승인선");
        }

        @Test
        @DisplayName("Given: description에 키워드 포함 / When: list 호출 / Then: 해당 템플릿 반환")
        void listWithKeywordInDescription() {
            ApprovalTemplateRoot t1 = createTestTemplateWithVersionAndDescription("템플릿1", "특별한 설명입니다");
            ApprovalTemplateRoot t2 = createTestTemplateWithVersion("템플릿2");
            given(templateRepo.findAll()).willReturn(List.of(t1, t2));

            List<ApprovalTemplateRootResponse> result = service.list("특별한", false);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("템플릿1");
        }

        @Test
        @DisplayName("Given: 빈 키워드 / When: list 호출 / Then: 모든 템플릿 반환")
        void listWithBlankKeyword() {
            ApprovalTemplateRoot t1 = createTestTemplateWithVersion("템플릿1");
            ApprovalTemplateRoot t2 = createTestTemplateWithVersion("템플릿2");
            given(templateRepo.findAll()).willReturn(List.of(t1, t2));

            List<ApprovalTemplateRootResponse> result = service.list("   ", false);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Given: 동일 displayOrder / When: list 호출 / Then: 이름순 정렬")
        void listSortsByNameWhenSameDisplayOrder() {
            ApprovalTemplateRoot t1 = createTestTemplateWithVersionAndOrder("Zebra", 0);
            ApprovalTemplateRoot t2 = createTestTemplateWithVersionAndOrder("Alpha", 0);
            given(templateRepo.findAll()).willReturn(List.of(t1, t2));

            List<ApprovalTemplateRootResponse> result = service.list(null, false);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("Alpha");
            assertThat(result.get(1).name()).isEqualTo("Zebra");
        }

        @Test
        @DisplayName("Given: null name 템플릿 / When: list 정렬 / Then: null이 뒤로 정렬됨")
        void listSortsNullNameToEnd() {
            ApprovalTemplateRoot t1 = createTestTemplateWithVersionAndOrder(null, 0);
            ApprovalTemplateRoot t2 = createTestTemplateWithVersionAndOrder("Alpha", 0);
            given(templateRepo.findAll()).willReturn(List.of(t1, t2));

            List<ApprovalTemplateRootResponse> result = service.list(null, false);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("Alpha");
            assertThat(result.get(1).name()).isNull();
        }

        @Test
        @DisplayName("Given: 두 템플릿 모두 null name / When: list 정렬 / Then: 둘 다 포함됨")
        void listSortsBothNullNames() {
            ApprovalTemplateRoot t1 = createTestTemplateWithVersionAndOrder(null, 0);
            ApprovalTemplateRoot t2 = createTestTemplateWithVersionAndOrder(null, 0);
            given(templateRepo.findAll()).willReturn(List.of(t1, t2));

            List<ApprovalTemplateRootResponse> result = service.list(null, false);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Given: 비활성 템플릿이 포함될 때 / When: activeOnly=false / Then: 모든 템플릿 반환")
        void listIncludesInactiveWhenRequested() {
            ApprovalTemplateRoot active = createTestTemplateWithVersion("활성");
            ApprovalTemplateRoot inactive = createInactiveTemplateWithVersion("비활성");
            given(templateRepo.findAll()).willReturn(List.of(active, inactive));

            List<ApprovalTemplateRootResponse> result = service.list(null, false);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Given: 현재 버전이 없는 템플릿 / When: list 호출 / Then: 해당 템플릿은 제외됨")
        void listExcludesTemplatesWithoutCurrentVersion() {
            ApprovalTemplateRoot withVersion = createTestTemplateWithVersion("버전있음");
            ApprovalTemplateRoot withoutVersion = ApprovalTemplateRoot.create(OffsetDateTime.now());
            given(templateRepo.findAll()).willReturn(List.of(withVersion, withoutVersion));

            List<ApprovalTemplateRootResponse> result = service.list(null, false);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("버전있음");
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("Given: 존재하는 템플릿 ID / When: getById 호출 / Then: 템플릿 반환")
        void getByIdReturnsTemplate() {
            UUID id = UUID.randomUUID();
            ApprovalTemplateRoot template = createTestTemplateWithVersion("테스트 템플릿");
            given(templateRepo.findById(id)).willReturn(Optional.of(template));

            ApprovalTemplateRootResponse result = service.getById(id);

            assertThat(result.name()).isEqualTo("테스트 템플릿");
        }

        @Test
        @DisplayName("Given: 존재하지 않는 템플릿 ID / When: getById 호출 / Then: ApprovalTemplateRootNotFoundException 발생")
        void getByIdThrowsNotFoundException() {
            UUID id = UUID.randomUUID();
            given(templateRepo.findById(id)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(id))
                    .isInstanceOf(ApprovalTemplateRootNotFoundException.class)
                    .hasMessage("승인선 템플릿을 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTemplate {

        @Test
        @DisplayName("Given: 유효한 요청 / When: create 호출 / Then: 템플릿 생성 및 SCD Type 2 버전 기록")
        void createTemplateAndRecordsVersion() {
            given(templateRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            ApprovalTemplateRootRequest request = new ApprovalTemplateRootRequest(
                    "새 템플릿", 1, "설명", true,
                    List.of(new ApprovalTemplateStepRequest(1, "TEAM_LEADER", false)));

            service.create(request, testContext());

            verify(templateRepo).save(any());
            verify(versionService).createInitialVersion(any(), eq(request), any(), any());
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTemplate {

        @Test
        @DisplayName("Given: 존재하는 템플릿 / When: update 호출 / Then: SCD Type 2 버전 기록")
        void updateTemplateAndRecordsVersion() {
            UUID id = UUID.randomUUID();
            ApprovalTemplateRoot template = createTestTemplateWithVersion("기존 템플릿");
            given(templateRepo.findById(id)).willReturn(Optional.of(template));

            ApprovalTemplateRootRequest request = new ApprovalTemplateRootRequest(
                    "수정된 템플릿", 5, "수정된 설명", true,
                    List.of(new ApprovalTemplateStepRequest(1, "TEAM_LEADER", false)));

            service.update(id, request, testContext());

            verify(versionService).createUpdateVersion(eq(template), eq(request), any(), any());
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTemplate {

        @Test
        @DisplayName("Given: 존재하는 템플릿 / When: delete 호출 / Then: SCD Type 2 삭제 버전 기록")
        void deleteSoftDeletesAndRecordsVersion() {
            UUID id = UUID.randomUUID();
            ApprovalTemplateRoot template = createTestTemplateWithVersion("삭제할 템플릿");
            given(templateRepo.findById(id)).willReturn(Optional.of(template));

            service.delete(id, testContext());

            verify(versionService).createDeleteVersion(eq(template), any(), any());
        }
    }

    @Nested
    @DisplayName("activate")
    class ActivateTemplate {

        @Test
        @DisplayName("Given: 비활성 템플릿 / When: activate 호출 / Then: SCD Type 2 복원 버전 기록")
        void activateRestoresAndRecordsVersion() {
            UUID id = UUID.randomUUID();
            ApprovalTemplateRoot template = createInactiveTemplateWithVersion("비활성 템플릿");
            given(templateRepo.findById(id)).willReturn(Optional.of(template));

            service.activate(id, testContext());

            verify(versionService).createRestoreVersion(eq(template), any(), any());
        }
    }

    @Nested
    @DisplayName("getHistory")
    class GetHistory {

        @Test
        @DisplayName("Given: 이력이 있는 템플릿 / When: getHistory 호출 / Then: SCD Type 2 버전 이력 반환")
        void getHistoryReturnsScdVersionHistory() {
            UUID templateId = UUID.randomUUID();
            VersionHistoryResponse v1 = new VersionHistoryResponse(
                    UUID.randomUUID(), templateId, 1,
                    OffsetDateTime.now().minusDays(1), OffsetDateTime.now(),
                    "템플릿", 0, "설명", true,
                    VersionStatus.HISTORICAL, ChangeAction.CREATE,
                    null, "user1", "사용자1", OffsetDateTime.now().minusDays(1),
                    null, null, List.of());
            VersionHistoryResponse v2 = new VersionHistoryResponse(
                    UUID.randomUUID(), templateId, 2,
                    OffsetDateTime.now(), null,
                    "수정된 템플릿", 0, "수정 설명", true,
                    VersionStatus.PUBLISHED, ChangeAction.UPDATE,
                    "변경 사유", "user1", "사용자1", OffsetDateTime.now(),
                    null, null, List.of());
            given(versionService.getVersionHistory(templateId)).willReturn(List.of(v2, v1));

            List<VersionHistoryResponse> result = service.getHistory(templateId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).version()).isEqualTo(2);
            assertThat(result.get(0).changeAction()).isEqualTo(ChangeAction.UPDATE);
            assertThat(result.get(1).version()).isEqualTo(1);
            assertThat(result.get(1).changeAction()).isEqualTo(ChangeAction.CREATE);
        }
    }

    @Nested
    @DisplayName("updateDisplayOrders")
    class UpdateDisplayOrders {

        @Test
        @DisplayName("Given: 여러 템플릿 / When: updateDisplayOrders 호출 / Then: 순서가 변경된 것만 버전 기록")
        void updateDisplayOrdersUpdatesMultipleTemplates() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            ApprovalTemplateRoot t1 = createTestTemplateWithVersion("템플릿1");
            ApprovalTemplateRoot t2 = createTestTemplateWithVersion("템플릿2");

            given(templateRepo.findById(id1)).willReturn(Optional.of(t1));
            given(templateRepo.findById(id2)).willReturn(Optional.of(t2));

            DisplayOrderUpdateRequest request = new DisplayOrderUpdateRequest(List.of(
                    new DisplayOrderUpdateRequest.DisplayOrderItem(id1, 10),
                    new DisplayOrderUpdateRequest.DisplayOrderItem(id2, 20)));

            List<ApprovalTemplateRootResponse> result = service.updateDisplayOrders(request, testContext());

            assertThat(result).hasSize(2);
            // 순서가 변경되었으므로 버전 서비스가 호출되어야 함 (0 -> 10, 0 -> 20)
            verify(versionService).createUpdateVersion(eq(t1), any(), any(), any());
            verify(versionService).createUpdateVersion(eq(t2), any(), any(), any());
        }

        @Test
        @DisplayName("Given: 순서가 변경되지 않은 템플릿 / When: updateDisplayOrders 호출 / Then: 버전 기록 안함")
        void noVersionRecordWhenDisplayOrderUnchanged() {
            UUID id1 = UUID.randomUUID();
            ApprovalTemplateRoot t1 = createTestTemplateWithVersion("템플릿1");
            // 이미 displayOrder가 0임

            given(templateRepo.findById(id1)).willReturn(Optional.of(t1));

            DisplayOrderUpdateRequest request = new DisplayOrderUpdateRequest(List.of(
                    new DisplayOrderUpdateRequest.DisplayOrderItem(id1, 0))); // 동일한 순서

            List<ApprovalTemplateRootResponse> result = service.updateDisplayOrders(request, testContext());

            assertThat(result).hasSize(1);
            // 순서가 변경되지 않았으므로 버전 서비스가 호출되지 않아야 함
            verify(versionService, never()).createUpdateVersion(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Given: 현재 버전이 없는 템플릿 / When: updateDisplayOrders 호출 / Then: 예외 발생")
        void throwsExceptionWhenNoCurrentVersion() {
            UUID id = UUID.randomUUID();
            ApprovalTemplateRoot template = ApprovalTemplateRoot.create(OffsetDateTime.now());
            given(templateRepo.findById(id)).willReturn(Optional.of(template));

            DisplayOrderUpdateRequest request = new DisplayOrderUpdateRequest(List.of(
                    new DisplayOrderUpdateRequest.DisplayOrderItem(id, 10)));

            assertThatThrownBy(() -> service.updateDisplayOrders(request, testContext()))
                    .isInstanceOf(ApprovalTemplateRootNotFoundException.class)
                    .hasMessageContaining("현재 버전이 없습니다");
        }
    }
}
