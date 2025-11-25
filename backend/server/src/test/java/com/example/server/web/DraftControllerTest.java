package com.example.server.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.PermissionDeniedException;
import com.example.auth.permission.PermissionEvaluator;
import com.example.auth.permission.context.AuthContext;
import com.example.auth.permission.context.AuthContextHolder;
import com.example.common.security.RowScope;
import com.example.draft.application.DraftApplicationService;
import com.example.draft.application.response.DraftAttachmentResponse;
import com.example.draft.application.response.DraftApprovalStepResponse;
import com.example.draft.application.response.DraftResponse;
import com.example.draft.domain.DraftApprovalState;
import com.example.draft.domain.DraftStatus;
import com.example.dw.application.DwOrganizationNode;
import com.example.server.config.JpaConfig;
import com.example.server.config.SecurityConfig;
import com.example.server.security.JwtAuthenticationFilter;
import com.example.server.security.RestAccessDeniedHandler;
import com.example.server.security.RestAuthenticationEntryPoint;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = DraftController.class,
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class,
                        RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class, JpaConfig.class}))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("DraftController 테스트")
class DraftControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DraftApplicationService draftApplicationService;

    @MockBean
    private PermissionEvaluator permissionEvaluator;

    @MockBean
    private com.example.dw.application.DwOrganizationQueryService organizationQueryService;

    @BeforeEach
    void setUp() {
        AuthContextHolder.set(new AuthContext("writer", "ORG-001", "DEFAULT",
                FeatureCode.DRAFT, ActionCode.DRAFT_CREATE, RowScope.ALL, java.util.Map.of()));
    }

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    @DisplayName("Given 기안 생성 요청 When POST 호출 Then DraftResponse를 반환한다")
    void givenCreateRequest_whenPosting_thenReturnsDraft() throws Exception {
        DraftResponse response = sampleResponse(DraftStatus.DRAFT);
        given(draftApplicationService.createDraft(any(), eq("writer"), eq("ORG-001"))).willReturn(response);
        given(permissionEvaluator.evaluate(FeatureCode.NOTICE, ActionCode.DRAFT_CREATE)).willReturn(null);
        denyAuditAccess();

        mockMvc.perform(post("/api/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.example.draft.application.request.DraftCreateRequest(
                                "제목", "본문", "NOTICE", UUID.randomUUID(), UUID.randomUUID(), "{}", java.util.List.of(), null, java.util.Map.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("제목"));

        verify(draftApplicationService).createDraft(any(), eq("writer"), eq("ORG-001"));
    }

    @Test
    @DisplayName("Given 기안 ID When submit 호출 Then 서비스에서 상신 처리한다")
    void givenDraft_whenSubmitting_thenInvokesService() throws Exception {
        DraftResponse snapshot = sampleResponse(DraftStatus.DRAFT);
        given(permissionEvaluator.evaluate(FeatureCode.NOTICE, ActionCode.DRAFT_SUBMIT)).willReturn(null);
        given(draftApplicationService.getDraft(any(), eq("ORG-001"), eq("writer"), eq(false))).willReturn(snapshot);
        DraftResponse submitted = sampleResponse(DraftStatus.IN_REVIEW);
        given(draftApplicationService.submitDraft(any(), eq("writer"), eq("ORG-001"))).willReturn(submitted);
        denyAuditAccess();

        mockMvc.perform(post("/api/drafts/" + snapshot.id() + "/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(DraftStatus.IN_REVIEW.name()));

        verify(draftApplicationService).submitDraft(snapshot.id(), "writer", "ORG-001");
    }

    @Test
    @DisplayName("Given 승인 요청 When POST 호출 Then 해당 단계를 승인한다")
    void givenApprovalRequest_whenPosting_thenApproves() throws Exception {
        DraftResponse snapshot = sampleResponse(DraftStatus.IN_REVIEW);
        given(permissionEvaluator.evaluate(FeatureCode.NOTICE, ActionCode.DRAFT_APPROVE)).willReturn(null);
        given(draftApplicationService.getDraft(any(), eq("ORG-001"), eq("writer"), eq(false))).willReturn(snapshot);
        DraftResponse approved = sampleResponse(DraftStatus.APPROVED);
        given(draftApplicationService.approve(any(), any(), eq("writer"), eq("ORG-001"), eq(false)))
                .willReturn(approved);
        denyAuditAccess();

        mockMvc.perform(post("/api/drafts/" + snapshot.id() + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.example.draft.application.request.DraftDecisionRequest(
                                UUID.randomUUID(), "승인"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(DraftStatus.APPROVED.name()));

        verify(draftApplicationService).approve(eq(snapshot.id()), any(), eq("writer"), eq("ORG-001"), eq(false));
    }

    @Test
    @DisplayName("Given 반려 요청 When POST 호출 Then 기안이 반려 상태가 된다")
    void givenRejectRequest_whenPosting_thenRejects() throws Exception {
        DraftResponse snapshot = sampleResponse(DraftStatus.IN_REVIEW);
        given(permissionEvaluator.evaluate(FeatureCode.NOTICE, ActionCode.DRAFT_APPROVE)).willReturn(null);
        given(draftApplicationService.getDraft(any(), eq("ORG-001"), eq("writer"), eq(false))).willReturn(snapshot);
        DraftResponse rejected = sampleResponse(DraftStatus.REJECTED);
        given(draftApplicationService.reject(any(), any(), eq("writer"), eq("ORG-001"), eq(false)))
                .willReturn(rejected);
        denyAuditAccess();

        mockMvc.perform(post("/api/drafts/" + snapshot.id() + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.example.draft.application.request.DraftDecisionRequest(
                                UUID.randomUUID(), "반려"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(DraftStatus.REJECTED.name()));

        verify(draftApplicationService).reject(eq(snapshot.id()), any(), eq("writer"), eq("ORG-001"), eq(false));
    }

    @Test
    @DisplayName("Given 취소 요청 When POST 호출 Then 기안이 취소된다")
    void givenCancelRequest_whenPosting_thenCancels() throws Exception {
        DraftResponse snapshot = sampleResponse(DraftStatus.IN_REVIEW);
        given(permissionEvaluator.evaluate(FeatureCode.NOTICE, ActionCode.DRAFT_CANCEL)).willReturn(null);
        given(draftApplicationService.getDraft(snapshot.id(), "ORG-001", "writer", false)).willReturn(snapshot);
        DraftResponse cancelled = sampleResponse(DraftStatus.CANCELLED);
        given(draftApplicationService.cancel(any(), eq("writer"), eq("ORG-001"))).willReturn(cancelled);
        denyAuditAccess();

        mockMvc.perform(post("/api/drafts/" + snapshot.id() + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(DraftStatus.CANCELLED.name()));

        verify(draftApplicationService).cancel(snapshot.id(), "writer", "ORG-001");
    }

    @Test
    @DisplayName("Given 회수 요청 When POST 호출 Then 기안이 WITHDRAWN 된다")
    void givenWithdrawRequest_whenPosting_thenWithdrawn() throws Exception {
        DraftResponse snapshot = sampleResponse(DraftStatus.IN_REVIEW);
        given(permissionEvaluator.evaluate(FeatureCode.NOTICE, ActionCode.DRAFT_WITHDRAW)).willReturn(null);
        given(draftApplicationService.getDraft(snapshot.id(), "ORG-001", "writer", false)).willReturn(snapshot);
        DraftResponse withdrawn = sampleResponse(DraftStatus.WITHDRAWN);
        given(draftApplicationService.withdraw(any(), eq("writer"), eq("ORG-001"))).willReturn(withdrawn);
        denyAuditAccess();

        mockMvc.perform(post("/api/drafts/" + snapshot.id() + "/withdraw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(DraftStatus.WITHDRAWN.name()));

        verify(draftApplicationService).withdraw(snapshot.id(), "writer", "ORG-001");
    }

    @Test
    @DisplayName("Given 재상신 요청 When POST 호출 Then 기안이 재상신된다")
    void givenResubmitRequest_whenPosting_thenResubmits() throws Exception {
        DraftResponse snapshot = sampleResponse(DraftStatus.WITHDRAWN);
        given(permissionEvaluator.evaluate(FeatureCode.NOTICE, ActionCode.DRAFT_RESUBMIT)).willReturn(null);
        given(draftApplicationService.getDraft(snapshot.id(), "ORG-001", "writer", false)).willReturn(snapshot);
        DraftResponse resubmitted = sampleResponse(DraftStatus.IN_REVIEW);
        given(draftApplicationService.resubmit(any(), eq("writer"), eq("ORG-001"))).willReturn(resubmitted);
        denyAuditAccess();

        mockMvc.perform(post("/api/drafts/" + snapshot.id() + "/resubmit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(DraftStatus.IN_REVIEW.name()));

        verify(draftApplicationService).resubmit(snapshot.id(), "writer", "ORG-001");
    }

    @Test
    @DisplayName("Given 위임 요청 When POST 호출 Then 위임 대상이 설정된다")
    void givenDelegateRequest_whenPosting_thenDelegates() throws Exception {
        DraftResponse snapshot = sampleResponse(DraftStatus.IN_REVIEW);
        given(permissionEvaluator.evaluate(FeatureCode.NOTICE, ActionCode.DRAFT_DELEGATE)).willReturn(null);
        given(draftApplicationService.getDraft(snapshot.id(), "ORG-001", "writer", false)).willReturn(snapshot);
        DraftApprovalStepResponse delegatedStep = new DraftApprovalStepResponse(UUID.randomUUID(), 1, "GROUP-A",
                "1차", DraftApprovalState.IN_PROGRESS, null, OffsetDateTime.now(ZoneOffset.UTC), "위임", "delegatee", OffsetDateTime.now(ZoneOffset.UTC));
        DraftResponse delegated = new DraftResponse(snapshot.id(), "제목", "본문", "NOTICE", "ORG-001", "writer",
                DraftStatus.IN_REVIEW, null, "TEMPLATE", "FORM", 1, "{}", "{\"field\":\"value\"}",
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC),
                null, null, null, null, List.of(delegatedStep), snapshot.attachments(), null, null);
        given(draftApplicationService.delegate(any(), any(), eq("delegatee"), eq("writer"), eq("ORG-001"), eq(false)))
                .willReturn(delegated);
        denyAuditAccess();

        mockMvc.perform(post("/api/drafts/" + snapshot.id() + "/delegate")
                        .param("delegatedTo", "delegatee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.example.draft.application.request.DraftDecisionRequest(
                                UUID.randomUUID(), "위임 부탁"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalSteps[0].delegatedTo").value("delegatee"));

        verify(draftApplicationService).delegate(eq(snapshot.id()), any(), eq("delegatee"), eq("writer"), eq("ORG-001"), eq(false));
    }

    @Test
    @DisplayName("Given 감사 권한이 없을 때 When GET 호출 Then 자신의 조직 기안만 조회된다")
    void givenGetRequest_whenAuditDenied_thenReadsOwnOrg() throws Exception {
        DraftResponse snapshot = sampleResponse(DraftStatus.DRAFT);
        given(draftApplicationService.getDraft(snapshot.id(), "ORG-001", "writer", false)).willReturn(snapshot);
        denyAuditAccess();

        mockMvc.perform(get("/api/drafts/" + snapshot.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(snapshot.id().toString()));
    }

    @Test
    @DisplayName("Given 감사 권한 When 타 조직 기안 조회 Then 감사 플래그로 조회된다")
    void givenAuditPermission_whenGettingOtherOrgDraft_thenSetsAuditFlag() throws Exception {
        DraftResponse snapshot = sampleResponseWithOrg("ORG-999", DraftStatus.DRAFT);
        given(permissionEvaluator.evaluate(FeatureCode.DRAFT, ActionCode.DRAFT_AUDIT)).willReturn(null);
        given(draftApplicationService.getDraft(snapshot.id(), "ORG-001", "writer", true)).willReturn(snapshot);

        mockMvc.perform(get("/api/drafts/" + snapshot.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationCode").value("ORG-999"));

        verify(draftApplicationService).getDraft(snapshot.id(), "ORG-001", "writer", true);
    }

    @Test
    @DisplayName("Given 리스트 요청 When GET 호출 Then RowScope 조직으로 필터링한다")
    void givenListRequest_whenListing_thenFiltersByRowScope() throws Exception {
        AuthContextHolder.set(new AuthContext("writer", "ORG-001", "DEFAULT",
                FeatureCode.DRAFT, ActionCode.DRAFT_READ, RowScope.ORG, java.util.Map.of()));
        given(organizationQueryService.getOrganizations(Pageable.unpaged(), RowScope.ORG, "ORG-001"))
                .willReturn(new PageImpl<>(List.of(sampleOrgNode("ORG-001"))));
        DraftResponse response = sampleResponse(DraftStatus.DRAFT);
        given(draftApplicationService.listDrafts(any(Pageable.class), eq(RowScope.ORG), eq("ORG-001"), eq(List.of("ORG-001")),
                isNull(), isNull(), isNull(), isNull()))
                .willReturn(new PageImpl<>(List.of(response)));
        denyAuditAccess();

        mockMvc.perform(get("/api/drafts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(response.id().toString()));
    }

    private void denyAuditAccess() {
        given(permissionEvaluator.evaluate(FeatureCode.DRAFT, ActionCode.DRAFT_AUDIT))
                .willThrow(new PermissionDeniedException("denied"));
    }

    private DraftResponse sampleResponse(DraftStatus status) {
        DraftApprovalStepResponse step = new DraftApprovalStepResponse(UUID.randomUUID(), 1, "GROUP-A",
                "1차", DraftApprovalState.IN_PROGRESS, null, OffsetDateTime.now(ZoneOffset.UTC), null, null, null);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<DraftAttachmentResponse> attachments = List.of(
                new DraftAttachmentResponse(UUID.randomUUID(), "evidence.pdf", "application/pdf", 1024L,
                        now, "writer"));
        return new DraftResponse(UUID.randomUUID(), "제목", "본문", "NOTICE", "ORG-001", "writer",
                status, null, "TEMPLATE", "FORM", 1, "{}", "{\"field\":\"value\"}",
                now, now,
                null, null, null, null, List.of(step), attachments, null, null);
    }

    private DraftResponse sampleResponseWithOrg(String org, DraftStatus status) {
        DraftApprovalStepResponse step = new DraftApprovalStepResponse(UUID.randomUUID(), 1, "GROUP-A",
                "1차", DraftApprovalState.IN_PROGRESS, null, OffsetDateTime.now(ZoneOffset.UTC), null, null, null);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<DraftAttachmentResponse> attachments = List.of(
                new DraftAttachmentResponse(UUID.randomUUID(), "plan.xlsx", "application/vnd.ms-excel", 2048L,
                        now, "writer"));
        return new DraftResponse(UUID.randomUUID(), "제목", "본문", "NOTICE", org, "writer",
                status, null, "TEMPLATE", "FORM", 2, "{\"schema\":true}", "{\"value\":true}",
                now, now,
                null, null, null, null, List.of(step), attachments, null, null);
    }

    private DwOrganizationNode sampleOrgNode(String code) {
        return new DwOrganizationNode(UUID.randomUUID(), code, 1, code, null,
                "ACTIVE", LocalDate.now(), null, null, OffsetDateTime.now(ZoneOffset.UTC));
    }
}
