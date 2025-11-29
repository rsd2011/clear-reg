package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.admin.maskingpolicy.dto.MaskingPolicyDraftRequest;
import com.example.admin.maskingpolicy.dto.MaskingPolicyHistoryResponse;
import com.example.admin.maskingpolicy.dto.MaskingPolicyRootRequest;
import com.example.admin.maskingpolicy.dto.MaskingPolicyRootResponse;
import com.example.admin.maskingpolicy.service.MaskingPolicyRootService;
import com.example.admin.maskingpolicy.service.MaskingPolicyVersioningService;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.admin.permission.exception.PermissionDeniedException;
import com.example.common.security.RowScope;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;

@DisplayName("MaskingPolicyRootController 테스트")
class MaskingPolicyRootControllerTest {

    MaskingPolicyRootService policyService = mock(MaskingPolicyRootService.class);
    MaskingPolicyVersioningService versionService = mock(MaskingPolicyVersioningService.class);
    MaskingPolicyRootController controller = new MaskingPolicyRootController(policyService, versionService);

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    private AuthContext setupContext() {
        AuthContext ctx = AuthContext.of("user", "ORG", "PG", null, null, RowScope.ALL);
        AuthContextHolder.set(ctx);
        return ctx;
    }

    private MaskingPolicyRootResponse createRootResponse(UUID id, String name) {
        return new MaskingPolicyRootResponse(
                id,
                "MP-" + id.toString().substring(0, 8),
                name,
                "설명",
                FeatureCode.DRAFT,
                ActionCode.READ,
                "PG001",
                "OG001",
                Set.of("NAME", "EMAIL"),
                true,
                false,
                100,
                true,
                Instant.now(),
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                1,
                false
        );
    }

    private MaskingPolicyHistoryResponse createHistoryResponse(UUID id, UUID policyId, int version,
                                                                 VersionStatus status, ChangeAction action,
                                                                 String name, Integer rollbackFromVersion) {
        return new MaskingPolicyHistoryResponse(
                id,
                policyId,
                version,
                OffsetDateTime.now().minusDays(version),
                status == VersionStatus.PUBLISHED ? null : OffsetDateTime.now(),
                name,
                "설명",
                FeatureCode.DRAFT,
                ActionCode.READ,
                "PG001",
                "OG001",
                Set.of("NAME"),
                true,
                false,
                100,
                true,
                Instant.now(),
                null,
                status,
                action,
                action == ChangeAction.CREATE ? null : "변경 사유",
                "user",
                "사용자",
                OffsetDateTime.now(),
                rollbackFromVersion,
                null
        );
    }

    @Test
    @DisplayName("Given: 인증 컨텍스트가 없을 때 / When: createPolicy 호출 / Then: PermissionDeniedException 발생")
    void throwsWhenContextMissing() {
        AuthContextHolder.clear();

        MaskingPolicyRootRequest request = new MaskingPolicyRootRequest(
                "테스트 정책",
                "설명",
                FeatureCode.DRAFT,
                ActionCode.READ,
                "PG001",
                "OG001",
                Set.of("NAME"),
                true,
                false,
                100,
                true,
                null,
                null
        );

        assertThatThrownBy(() -> controller.createPolicy(request))
                .isInstanceOf(PermissionDeniedException.class);
    }

    // ==========================================================================
    // CRUD API 테스트
    // ==========================================================================

    @Nested
    @DisplayName("listPolicies")
    class ListPolicies {

        @Test
        @DisplayName("Given: 컨텍스트 설정됨 / When: listPolicies 호출 / Then: 서비스에 파라미터 전달 및 결과 반환")
        void listPoliciesReturns200() {
            setupContext();

            UUID id = UUID.randomUUID();
            when(policyService.list(any(), anyBoolean())).thenReturn(List.of(createRootResponse(id, "정책")));

            List<MaskingPolicyRootResponse> result = controller.listPolicies("키워드", true);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("정책");
            verify(policyService).list(eq("키워드"), eq(true));
        }
    }

    @Nested
    @DisplayName("listActivePolicies")
    class ListActivePolicies {

        @Test
        @DisplayName("Given: 활성 정책 존재 / When: listActivePolicies 호출 / Then: 활성 정책 목록 반환")
        void listActivePoliciesReturns200() {
            setupContext();

            UUID id = UUID.randomUUID();
            when(policyService.listActive()).thenReturn(List.of(createRootResponse(id, "활성 정책")));

            List<MaskingPolicyRootResponse> result = controller.listActivePolicies();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).active()).isTrue();
            verify(policyService).listActive();
        }
    }

    @Nested
    @DisplayName("listByFeatureCode")
    class ListByFeatureCode {

        @Test
        @DisplayName("Given: 특정 FeatureCode의 정책 존재 / When: listByFeatureCode 호출 / Then: 해당 정책 목록 반환")
        void listByFeatureCodeReturns200() {
            setupContext();

            UUID id = UUID.randomUUID();
            when(policyService.listByFeatureCode(FeatureCode.DRAFT))
                    .thenReturn(List.of(createRootResponse(id, "DRAFT 정책")));

            List<MaskingPolicyRootResponse> result = controller.listByFeatureCode(FeatureCode.DRAFT);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).featureCode()).isEqualTo(FeatureCode.DRAFT);
            verify(policyService).listByFeatureCode(FeatureCode.DRAFT);
        }
    }

    @Nested
    @DisplayName("getPolicy")
    class GetPolicy {

        @Test
        @DisplayName("Given: 유효한 ID / When: getPolicy 호출 / Then: 정책 반환")
        void getPolicyReturns200() {
            setupContext();

            UUID id = UUID.randomUUID();
            when(policyService.getById(id)).thenReturn(createRootResponse(id, "조회 정책"));

            MaskingPolicyRootResponse result = controller.getPolicy(id);

            assertThat(result.id()).isEqualTo(id);
            assertThat(result.name()).isEqualTo("조회 정책");
            verify(policyService).getById(id);
        }
    }

    @Nested
    @DisplayName("getByPolicyCode")
    class GetByPolicyCode {

        @Test
        @DisplayName("Given: 유효한 policyCode / When: getByPolicyCode 호출 / Then: 정책 반환")
        void getByPolicyCodeReturns200() {
            setupContext();

            UUID id = UUID.randomUUID();
            String policyCode = "MP-001";
            MaskingPolicyRootResponse response = createRootResponse(id, "코드 조회 정책");
            when(policyService.getByPolicyCode(policyCode)).thenReturn(response);

            MaskingPolicyRootResponse result = controller.getByPolicyCode(policyCode);

            assertThat(result.name()).isEqualTo("코드 조회 정책");
            verify(policyService).getByPolicyCode(policyCode);
        }
    }

    @Nested
    @DisplayName("createPolicy")
    class CreatePolicy {

        @Test
        @DisplayName("Given: 유효한 요청 / When: createPolicy 호출 / Then: 201 Created 및 생성된 정책 반환")
        void createPolicyReturns201() {
            AuthContext ctx = setupContext();

            UUID id = UUID.randomUUID();
            MaskingPolicyRootRequest request = new MaskingPolicyRootRequest(
                    "새 정책",
                    "설명",
                    FeatureCode.DRAFT,
                    ActionCode.READ,
                    "PG001",
                    "OG001",
                    Set.of("NAME"),
                    true,
                    false,
                    100,
                    true,
                    null,
                    null
            );

            when(policyService.create(eq(request), eq(ctx))).thenReturn(createRootResponse(id, "새 정책"));

            MaskingPolicyRootResponse result = controller.createPolicy(request);

            assertThat(result.name()).isEqualTo("새 정책");
            verify(policyService).create(eq(request), eq(ctx));
        }
    }

    @Nested
    @DisplayName("updatePolicy")
    class UpdatePolicy {

        @Test
        @DisplayName("Given: 존재하는 정책 / When: updatePolicy 호출 / Then: 수정된 정책 반환")
        void updatePolicyReturns200() {
            AuthContext ctx = setupContext();

            UUID id = UUID.randomUUID();
            MaskingPolicyRootRequest request = new MaskingPolicyRootRequest(
                    "수정 정책",
                    "수정된 설명",
                    FeatureCode.APPROVAL,
                    ActionCode.UPDATE,
                    "PG002",
                    "OG002",
                    Set.of("EMAIL"),
                    true,
                    true,
                    50,
                    true,
                    null,
                    null
            );

            when(policyService.update(eq(id), eq(request), eq(ctx))).thenReturn(createRootResponse(id, "수정 정책"));

            MaskingPolicyRootResponse result = controller.updatePolicy(id, request);

            assertThat(result.name()).isEqualTo("수정 정책");
            verify(policyService).update(eq(id), eq(request), eq(ctx));
        }
    }

    @Nested
    @DisplayName("deletePolicy")
    class DeletePolicy {

        @Test
        @DisplayName("Given: 존재하는 정책 / When: deletePolicy 호출 / Then: 204 No Content")
        void deletePolicyReturns204() {
            AuthContext ctx = setupContext();

            UUID id = UUID.randomUUID();
            controller.deletePolicy(id);

            verify(policyService).delete(eq(id), eq(ctx));
        }
    }

    @Nested
    @DisplayName("activatePolicy")
    class ActivatePolicy {

        @Test
        @DisplayName("Given: 비활성 정책 / When: activatePolicy 호출 / Then: 활성화된 정책 반환")
        void activatePolicyReturns200() {
            AuthContext ctx = setupContext();

            UUID id = UUID.randomUUID();
            when(policyService.activate(eq(id), eq(ctx))).thenReturn(createRootResponse(id, "활성화 정책"));

            MaskingPolicyRootResponse result = controller.activatePolicy(id);

            assertThat(result.active()).isTrue();
            verify(policyService).activate(eq(id), eq(ctx));
        }
    }

    @Nested
    @DisplayName("getPolicyHistory")
    class GetPolicyHistory {

        @Test
        @DisplayName("Given: 이력이 있는 정책 / When: getPolicyHistory 호출 / Then: 버전 이력 목록 반환")
        void getHistoryReturns200() {
            setupContext();

            UUID policyId = UUID.randomUUID();
            MaskingPolicyHistoryResponse v1 = createHistoryResponse(
                    UUID.randomUUID(), policyId, 1,
                    VersionStatus.HISTORICAL, ChangeAction.CREATE, "정책", null);
            MaskingPolicyHistoryResponse v2 = createHistoryResponse(
                    UUID.randomUUID(), policyId, 2,
                    VersionStatus.PUBLISHED, ChangeAction.UPDATE, "수정된 정책", null);

            when(policyService.getHistory(policyId)).thenReturn(List.of(v2, v1));

            List<MaskingPolicyHistoryResponse> result = controller.getPolicyHistory(policyId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).changeAction()).isEqualTo(ChangeAction.UPDATE);
            verify(policyService).getHistory(policyId);
        }
    }

    // ==========================================================================
    // SCD Type 2 버전 관리 API 테스트
    // ==========================================================================

    @Nested
    @DisplayName("getVersionHistory")
    class GetVersionHistory {

        @Test
        @DisplayName("Given: 버전 이력이 있는 정책 / When: getVersionHistory 호출 / Then: 전체 버전 이력 목록 반환")
        void getVersionHistoryReturnsHistory() {
            setupContext();

            UUID policyId = UUID.randomUUID();
            MaskingPolicyHistoryResponse v1 = createHistoryResponse(
                    UUID.randomUUID(), policyId, 1,
                    VersionStatus.HISTORICAL, ChangeAction.CREATE, "정책", null);
            MaskingPolicyHistoryResponse v2 = createHistoryResponse(
                    UUID.randomUUID(), policyId, 2,
                    VersionStatus.PUBLISHED, ChangeAction.UPDATE, "수정 정책", null);

            when(versionService.getVersionHistory(policyId)).thenReturn(List.of(v2, v1));

            List<MaskingPolicyHistoryResponse> result = controller.getVersionHistory(policyId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).version()).isEqualTo(2);
            verify(versionService).getVersionHistory(policyId);
        }
    }

    @Nested
    @DisplayName("getVersion")
    class GetVersion {

        @Test
        @DisplayName("Given: 특정 버전 존재 / When: getVersion 호출 / Then: 해당 버전 상세 반환")
        void getVersionReturnsSpecificVersion() {
            setupContext();

            UUID policyId = UUID.randomUUID();
            MaskingPolicyHistoryResponse response = createHistoryResponse(
                    UUID.randomUUID(), policyId, 2,
                    VersionStatus.PUBLISHED, ChangeAction.UPDATE, "수정 정책", null);

            when(versionService.getVersion(policyId, 2)).thenReturn(response);

            MaskingPolicyHistoryResponse result = controller.getVersion(policyId, 2);

            assertThat(result.version()).isEqualTo(2);
            assertThat(result.name()).isEqualTo("수정 정책");
            verify(versionService).getVersion(policyId, 2);
        }
    }

    @Nested
    @DisplayName("getVersionAsOf")
    class GetVersionAsOf {

        @Test
        @DisplayName("Given: 특정 시점에 유효한 버전 / When: getVersionAsOf 호출 / Then: 해당 시점 버전 반환")
        void getVersionAsOfReturnsPointInTimeVersion() {
            setupContext();

            UUID policyId = UUID.randomUUID();
            OffsetDateTime asOf = OffsetDateTime.now().minusDays(5);
            MaskingPolicyHistoryResponse response = createHistoryResponse(
                    UUID.randomUUID(), policyId, 1,
                    VersionStatus.HISTORICAL, ChangeAction.CREATE, "정책", null);

            when(versionService.getVersionAsOf(policyId, asOf)).thenReturn(response);

            MaskingPolicyHistoryResponse result = controller.getVersionAsOf(policyId, asOf);

            assertThat(result.version()).isEqualTo(1);
            verify(versionService).getVersionAsOf(policyId, asOf);
        }
    }

    @Nested
    @DisplayName("rollbackToVersion")
    class RollbackToVersion {

        @Test
        @DisplayName("Given: 롤백 가능한 버전 / When: rollbackToVersion 호출 / Then: 새 버전 생성 후 반환")
        void rollbackCreatesNewVersion() {
            AuthContext ctx = setupContext();

            UUID policyId = UUID.randomUUID();
            Integer targetVersion = 1;
            String changeReason = "롤백 사유";
            MaskingPolicyHistoryResponse response = createHistoryResponse(
                    UUID.randomUUID(), policyId, 3,
                    VersionStatus.PUBLISHED, ChangeAction.ROLLBACK, "정책", 1);

            when(versionService.rollbackToVersion(eq(policyId), eq(targetVersion), eq(changeReason), eq(ctx)))
                    .thenReturn(response);

            MaskingPolicyHistoryResponse result = controller.rollbackToVersion(policyId, targetVersion, changeReason);

            assertThat(result.version()).isEqualTo(3);
            assertThat(result.changeAction()).isEqualTo(ChangeAction.ROLLBACK);
            assertThat(result.rollbackFromVersion()).isEqualTo(1);
            verify(versionService).rollbackToVersion(eq(policyId), eq(targetVersion), eq(changeReason), eq(ctx));
        }
    }

    // ==========================================================================
    // Draft/Published 시나리오 테스트
    // ==========================================================================

    @Nested
    @DisplayName("getDraft")
    class GetDraft {

        @Test
        @DisplayName("Given: 초안 존재 / When: getDraft 호출 / Then: 초안 정보 반환")
        void getDraftReturnsDraft() {
            setupContext();

            UUID policyId = UUID.randomUUID();
            MaskingPolicyHistoryResponse draft = createHistoryResponse(
                    UUID.randomUUID(), policyId, 2,
                    VersionStatus.DRAFT, ChangeAction.DRAFT, "초안 정책", null);

            when(versionService.getDraft(policyId)).thenReturn(draft);

            MaskingPolicyHistoryResponse result = controller.getDraft(policyId);

            assertThat(result.status()).isEqualTo(VersionStatus.DRAFT);
            verify(versionService).getDraft(policyId);
        }
    }

    @Nested
    @DisplayName("hasDraft")
    class HasDraft {

        @Test
        @DisplayName("Given: 초안 존재 / When: hasDraft 호출 / Then: true 반환")
        void hasDraftReturnsTrueWhenExists() {
            setupContext();

            UUID policyId = UUID.randomUUID();
            when(versionService.hasDraft(policyId)).thenReturn(true);

            boolean result = controller.hasDraft(policyId);

            assertThat(result).isTrue();
            verify(versionService).hasDraft(policyId);
        }

        @Test
        @DisplayName("Given: 초안 없음 / When: hasDraft 호출 / Then: false 반환")
        void hasDraftReturnsFalseWhenNotExists() {
            setupContext();

            UUID policyId = UUID.randomUUID();
            when(versionService.hasDraft(policyId)).thenReturn(false);

            boolean result = controller.hasDraft(policyId);

            assertThat(result).isFalse();
            verify(versionService).hasDraft(policyId);
        }
    }

    @Nested
    @DisplayName("saveDraft")
    class SaveDraft {

        @Test
        @DisplayName("Given: 유효한 초안 요청 / When: saveDraft 호출 / Then: 초안 저장 후 반환")
        void saveDraftCreatesDraft() {
            AuthContext ctx = setupContext();

            UUID policyId = UUID.randomUUID();
            MaskingPolicyDraftRequest request = new MaskingPolicyDraftRequest(
                    "초안 이름",
                    "초안 설명",
                    FeatureCode.DRAFT,
                    ActionCode.READ,
                    "PG001",
                    "OG001",
                    Set.of("NAME"),
                    true,
                    false,
                    100,
                    true,
                    null,
                    null,
                    "수정 중"
            );
            MaskingPolicyHistoryResponse response = createHistoryResponse(
                    UUID.randomUUID(), policyId, 2,
                    VersionStatus.DRAFT, ChangeAction.DRAFT, "초안 이름", null);

            when(versionService.saveDraft(eq(policyId), eq(request), eq(ctx))).thenReturn(response);

            MaskingPolicyHistoryResponse result = controller.saveDraft(policyId, request);

            assertThat(result.status()).isEqualTo(VersionStatus.DRAFT);
            assertThat(result.name()).isEqualTo("초안 이름");
            verify(versionService).saveDraft(eq(policyId), eq(request), eq(ctx));
        }
    }

    @Nested
    @DisplayName("publishDraft")
    class PublishDraft {

        @Test
        @DisplayName("Given: 초안 존재 / When: publishDraft 호출 / Then: 초안을 활성 버전으로 전환")
        void publishDraftActivatesDraft() {
            AuthContext ctx = setupContext();

            UUID policyId = UUID.randomUUID();
            MaskingPolicyHistoryResponse response = createHistoryResponse(
                    UUID.randomUUID(), policyId, 2,
                    VersionStatus.PUBLISHED, ChangeAction.UPDATE, "게시된 정책", null);

            when(versionService.publishDraft(eq(policyId), eq(ctx))).thenReturn(response);

            MaskingPolicyHistoryResponse result = controller.publishDraft(policyId);

            assertThat(result.status()).isEqualTo(VersionStatus.PUBLISHED);
            verify(versionService).publishDraft(eq(policyId), eq(ctx));
        }
    }

    @Nested
    @DisplayName("discardDraft")
    class DiscardDraft {

        @Test
        @DisplayName("Given: 초안 존재 / When: discardDraft 호출 / Then: 초안 삭제 완료")
        void discardDraftRemovesDraft() {
            setupContext();

            UUID policyId = UUID.randomUUID();

            controller.discardDraft(policyId);

            verify(versionService).discardDraft(policyId);
        }
    }

    @Nested
    @DisplayName("listWithDraft")
    class ListWithDraft {

        @Test
        @DisplayName("Given: 초안이 있는 정책 존재 / When: listWithDraft 호출 / Then: 해당 정책 목록 반환")
        void listWithDraftReturnsOnlyDrafts() {
            setupContext();

            UUID id = UUID.randomUUID();
            MaskingPolicyRootResponse response = new MaskingPolicyRootResponse(
                    id,
                    "MP-001",
                    "초안 있는 정책",
                    "설명",
                    FeatureCode.DRAFT,
                    ActionCode.READ,
                    "PG001",
                    "OG001",
                    Set.of("NAME"),
                    true,
                    false,
                    100,
                    true,
                    null,
                    null,
                    OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    1,
                    true  // hasDraft = true
            );
            when(policyService.listWithDraft()).thenReturn(List.of(response));

            List<MaskingPolicyRootResponse> result = controller.listWithDraft();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).hasDraft()).isTrue();
            verify(policyService).listWithDraft();
        }
    }
}
