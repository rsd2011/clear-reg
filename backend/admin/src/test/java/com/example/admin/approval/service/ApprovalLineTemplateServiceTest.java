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
import com.example.admin.approval.domain.ApprovalLineTemplate;
import com.example.admin.approval.exception.ApprovalGroupNotFoundException;
import com.example.admin.approval.exception.ApprovalLineTemplateNotFoundException;
import com.example.admin.approval.repository.ApprovalGroupRepository;
import com.example.admin.approval.repository.ApprovalLineTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.admin.approval.dto.ApprovalLineTemplateRequest;
import com.example.admin.approval.dto.ApprovalLineTemplateResponse;
import com.example.admin.approval.dto.ApprovalTemplateStepRequest;
import com.example.admin.approval.dto.DisplayOrderUpdateRequest;
import com.example.admin.approval.dto.TemplateCopyRequest;
import com.example.admin.approval.dto.TemplateCopyResponse;
import com.example.admin.approval.dto.VersionHistoryResponse;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;
import com.example.admin.permission.context.AuthContext;
import com.example.common.security.RowScope;

class ApprovalLineTemplateServiceTest {

    private ApprovalLineTemplateRepository templateRepo;
    private ApprovalGroupRepository groupRepo;
    private ApprovalLineTemplateVersionService versionService;
    private ApprovalLineTemplateService service;

    @BeforeEach
    void setUp() {
        templateRepo = mock(ApprovalLineTemplateRepository.class);
        groupRepo = mock(ApprovalGroupRepository.class);
        versionService = mock(ApprovalLineTemplateVersionService.class);
        service = new ApprovalLineTemplateService(templateRepo, groupRepo, versionService);
    }

    private AuthContext testContext() {
        return AuthContext.of("testuser", "ORG1", null, null, null, RowScope.ORG);
    }

    private ApprovalLineTemplate createTestTemplate(String name) {
        return ApprovalLineTemplate.create(name, 0, "설명", OffsetDateTime.now());
    }

    private ApprovalGroup createTestGroup(String code, String name) {
        return ApprovalGroup.create(code, name, "설명", 0, OffsetDateTime.now());
    }

    @Nested
    @DisplayName("list")
    class ListTemplates {

        @Test
        @DisplayName("Given: 템플릿 목록이 존재할 때 / When: list 호출 / Then: 모든 활성 템플릿 반환")
        void listReturnsAllActiveTemplates() {
            ApprovalLineTemplate t1 = createTestTemplate("템플릿1");
            ApprovalLineTemplate t2 = createTestTemplate("템플릿2");
            given(templateRepo.findAll()).willReturn(List.of(t1, t2));

            List<ApprovalLineTemplateResponse> result = service.list(null, true);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(ApprovalLineTemplateResponse::name)
                    .containsExactlyInAnyOrder("템플릿1", "템플릿2");
        }

        @Test
        @DisplayName("Given: 키워드가 주어졌을 때 / When: list 호출 / Then: 키워드로 필터링된 결과 반환")
        void listWithKeywordFiltersResults() {
            ApprovalLineTemplate t1 = createTestTemplate("기본 승인선");
            ApprovalLineTemplate t2 = createTestTemplate("특수 템플릿");
            given(templateRepo.findAll()).willReturn(List.of(t1, t2));

            List<ApprovalLineTemplateResponse> result = service.list("기본", true);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("기본 승인선");
        }

        @Test
        @DisplayName("Given: 비활성 템플릿이 포함될 때 / When: activeOnly=false / Then: 모든 템플릿 반환")
        void listIncludesInactiveWhenRequested() {
            ApprovalLineTemplate active = createTestTemplate("활성");
            ApprovalLineTemplate inactive = createTestTemplate("비활성");
            inactive.rename("비활성", 0, "설명", false, OffsetDateTime.now());
            given(templateRepo.findAll()).willReturn(List.of(active, inactive));

            List<ApprovalLineTemplateResponse> result = service.list(null, false);

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("Given: 존재하는 템플릿 ID / When: getById 호출 / Then: 템플릿 반환")
        void getByIdReturnsTemplate() {
            UUID id = UUID.randomUUID();
            ApprovalLineTemplate template = createTestTemplate("테스트 템플릿");
            given(templateRepo.findById(id)).willReturn(Optional.of(template));

            ApprovalLineTemplateResponse result = service.getById(id);

            assertThat(result.name()).isEqualTo("테스트 템플릿");
        }

        @Test
        @DisplayName("Given: 존재하지 않는 템플릿 ID / When: getById 호출 / Then: ApprovalLineTemplateNotFoundException 발생")
        void getByIdThrowsNotFoundException() {
            UUID id = UUID.randomUUID();
            given(templateRepo.findById(id)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(id))
                    .isInstanceOf(ApprovalLineTemplateNotFoundException.class)
                    .hasMessage("승인선 템플릿을 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTemplate {

        @Test
        @DisplayName("Given: 유효한 요청 / When: create 호출 / Then: 템플릿 생성 및 SCD Type 2 버전 기록")
        void createTemplateAndRecordsVersion() {
            ApprovalGroup group = createTestGroup("TEAM_LEADER", "팀장");
            given(groupRepo.findByGroupCode("TEAM_LEADER")).willReturn(Optional.of(group));
            given(templateRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            ApprovalLineTemplateRequest request = new ApprovalLineTemplateRequest(
                    "새 템플릿", 1, "설명", true,
                    List.of(new ApprovalTemplateStepRequest(1, "TEAM_LEADER")));

            ApprovalLineTemplateResponse result = service.create(request, testContext());

            assertThat(result.name()).isEqualTo("새 템플릿");
            assertThat(result.displayOrder()).isEqualTo(1);
            verify(templateRepo).save(any());
            verify(versionService).createInitialVersion(any(), eq(request), eq(testContext()), any());
        }

        @Test
        @DisplayName("Given: 존재하지 않는 승인그룹 코드 / When: create 호출 / Then: ApprovalGroupNotFoundException 발생")
        void createThrowsExceptionForInvalidGroup() {
            given(groupRepo.findByGroupCode("INVALID")).willReturn(Optional.empty());

            ApprovalLineTemplateRequest request = new ApprovalLineTemplateRequest(
                    "새 템플릿", 1, "설명", true,
                    List.of(new ApprovalTemplateStepRequest(1, "INVALID")));

            assertThatThrownBy(() -> service.create(request, testContext()))
                    .isInstanceOf(ApprovalGroupNotFoundException.class)
                    .hasMessageContaining("INVALID");
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTemplate {

        @Test
        @DisplayName("Given: 존재하는 템플릿 / When: update 호출 / Then: 템플릿 수정 및 SCD Type 2 버전 기록")
        void updateTemplateAndRecordsVersion() {
            UUID id = UUID.randomUUID();
            ApprovalLineTemplate template = createTestTemplate("기존 템플릿");
            ApprovalGroup group = createTestGroup("TEAM_LEADER", "팀장");

            given(templateRepo.findById(id)).willReturn(Optional.of(template));
            given(groupRepo.findByGroupCode("TEAM_LEADER")).willReturn(Optional.of(group));

            ApprovalLineTemplateRequest request = new ApprovalLineTemplateRequest(
                    "수정된 템플릿", 5, "수정된 설명", true,
                    List.of(new ApprovalTemplateStepRequest(1, "TEAM_LEADER")));

            ApprovalLineTemplateResponse result = service.update(id, request, testContext());

            assertThat(result.name()).isEqualTo("수정된 템플릿");
            assertThat(result.displayOrder()).isEqualTo(5);
            verify(versionService).createUpdateVersion(eq(template), eq(request), eq(testContext()), any());
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTemplate {

        @Test
        @DisplayName("Given: 존재하는 템플릿 / When: delete 호출 / Then: soft delete 수행 및 SCD Type 2 버전 기록")
        void deleteSoftDeletesAndRecordsVersion() {
            UUID id = UUID.randomUUID();
            ApprovalLineTemplate template = createTestTemplate("삭제할 템플릿");
            given(templateRepo.findById(id)).willReturn(Optional.of(template));

            service.delete(id, testContext());

            assertThat(template.isActive()).isFalse();
            verify(versionService).createDeleteVersion(eq(template), eq(testContext()), any());
        }
    }

    @Nested
    @DisplayName("activate")
    class ActivateTemplate {

        @Test
        @DisplayName("Given: 비활성 템플릿 / When: activate 호출 / Then: 활성화 및 SCD Type 2 버전 기록")
        void activateRestoresAndRecordsVersion() {
            UUID id = UUID.randomUUID();
            ApprovalLineTemplate template = createTestTemplate("비활성 템플릿");
            template.rename("비활성 템플릿", 0, "설명", false, OffsetDateTime.now());
            given(templateRepo.findById(id)).willReturn(Optional.of(template));

            ApprovalLineTemplateResponse result = service.activate(id, testContext());

            assertThat(result.active()).isTrue();
            verify(versionService).createRestoreVersion(eq(template), eq(testContext()), any());
        }
    }

    @Nested
    @DisplayName("copy")
    class CopyTemplate {

        @Test
        @DisplayName("Given: 존재하는 템플릿 / When: copy 호출 / Then: 새 템플릿 생성 및 SCD Type 2 버전 기록")
        void copyCreatesNewTemplateWithVersion() {
            ApprovalLineTemplate source = createTestTemplate("원본 템플릿");
            UUID sourceId = source.getId();
            ApprovalGroup group = createTestGroup("TEAM_LEADER", "팀장");
            source.addStep(1, group);

            given(templateRepo.findById(sourceId)).willReturn(Optional.of(source));
            given(templateRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            TemplateCopyRequest request = new TemplateCopyRequest("복사된 템플릿", "복사 설명");

            TemplateCopyResponse result = service.copy(sourceId, request, testContext());

            assertThat(result.name()).isEqualTo("복사된 템플릿");
            assertThat(result.copiedFrom().id()).isEqualTo(sourceId);
            verify(templateRepo).save(any());
            verify(versionService).createCopyVersion(any(), eq(source), eq("복사된 템플릿"), eq("복사 설명"), eq(testContext()), any());
        }

        @Test
        @DisplayName("Given: 템플릿 복사 시 / When: steps가 있으면 / Then: 모든 steps 복제")
        void copyIncludesAllSteps() {
            ApprovalLineTemplate source = createTestTemplate("원본");
            UUID sourceId = source.getId();
            ApprovalGroup group1 = createTestGroup("TEAM_LEADER", "팀장");
            ApprovalGroup group2 = createTestGroup("DEPT_HEAD", "부서장");
            source.addStep(1, group1);
            source.addStep(2, group2);

            given(templateRepo.findById(sourceId)).willReturn(Optional.of(source));
            given(templateRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            TemplateCopyRequest request = new TemplateCopyRequest("복사본", null);

            TemplateCopyResponse result = service.copy(sourceId, request, testContext());

            assertThat(result.steps()).hasSize(2);
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
                    null, null, null, List.of());
            VersionHistoryResponse v2 = new VersionHistoryResponse(
                    UUID.randomUUID(), templateId, 2,
                    OffsetDateTime.now(), null,
                    "수정된 템플릿", 0, "수정 설명", true,
                    VersionStatus.PUBLISHED, ChangeAction.UPDATE,
                    "변경 사유", "user1", "사용자1", OffsetDateTime.now(),
                    null, null, null, List.of());
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
        @DisplayName("Given: 여러 템플릿 / When: updateDisplayOrders 호출 / Then: 모든 순서 업데이트 및 변경된 것만 버전 기록")
        void updateDisplayOrdersUpdatesMultipleTemplates() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            ApprovalLineTemplate t1 = createTestTemplate("템플릿1");
            ApprovalLineTemplate t2 = createTestTemplate("템플릿2");

            given(templateRepo.findById(id1)).willReturn(Optional.of(t1));
            given(templateRepo.findById(id2)).willReturn(Optional.of(t2));

            DisplayOrderUpdateRequest request = new DisplayOrderUpdateRequest(List.of(
                    new DisplayOrderUpdateRequest.DisplayOrderItem(id1, 10),
                    new DisplayOrderUpdateRequest.DisplayOrderItem(id2, 20)));

            List<ApprovalLineTemplateResponse> result = service.updateDisplayOrders(request, testContext());

            assertThat(result).hasSize(2);
            assertThat(result).anyMatch(r -> r.name().equals("템플릿1") && r.displayOrder() == 10);
            assertThat(result).anyMatch(r -> r.name().equals("템플릿2") && r.displayOrder() == 20);
            // 순서가 변경되었으므로 버전 서비스가 호출되어야 함
            verify(versionService).createUpdateVersion(eq(t1), any(), eq(testContext()), any());
            verify(versionService).createUpdateVersion(eq(t2), any(), eq(testContext()), any());
        }

        @Test
        @DisplayName("Given: 순서가 변경되지 않은 템플릿 / When: updateDisplayOrders 호출 / Then: 버전 기록 안함")
        void noVersionRecordWhenDisplayOrderUnchanged() {
            UUID id1 = UUID.randomUUID();
            ApprovalLineTemplate t1 = createTestTemplate("템플릿1");
            // 이미 displayOrder가 0임

            given(templateRepo.findById(id1)).willReturn(Optional.of(t1));

            DisplayOrderUpdateRequest request = new DisplayOrderUpdateRequest(List.of(
                    new DisplayOrderUpdateRequest.DisplayOrderItem(id1, 0))); // 동일한 순서

            List<ApprovalLineTemplateResponse> result = service.updateDisplayOrders(request, testContext());

            assertThat(result).hasSize(1);
            // 순서가 변경되지 않았으므로 버전 서비스가 호출되지 않아야 함
            verify(versionService, never()).createUpdateVersion(any(), any(), any(), any());
        }
    }
}
