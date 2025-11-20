package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.example.draft.application.request.DraftAttachmentRequest;
import com.example.draft.application.request.DraftCreateRequest;
import com.example.draft.application.request.DraftDecisionRequest;
import com.example.draft.application.response.DraftResponse;
import com.example.draft.domain.ApprovalGroup;
import com.example.draft.domain.ApprovalGroupMember;
import com.example.draft.domain.ApprovalLineTemplate;
import com.example.draft.domain.Draft;
import com.example.draft.domain.DraftFormTemplate;
import com.example.draft.domain.DraftStatus;
import com.example.draft.domain.exception.DraftAccessDeniedException;
import com.example.draft.domain.repository.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.ApprovalGroupMemberRepository;
import com.example.draft.domain.repository.ApprovalGroupRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;
import com.example.draft.domain.repository.DraftRepository;
import com.example.draft.application.notification.DraftNotificationService;
import com.example.common.security.RowScope;

@ExtendWith(MockitoExtension.class)
class DraftApplicationServiceTest {

    private static final String ORG = "ORG-001";
    private static final OffsetDateTime NOW = OffsetDateTime.ofInstant(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private DraftRepository draftRepository;

    @Mock
    private ApprovalLineTemplateRepository templateRepository;

    @Mock
    private DraftFormTemplateRepository formTemplateRepository;

    @Mock
    private ApprovalGroupRepository approvalGroupRepository;

    @Mock
    private ApprovalGroupMemberRepository approvalGroupMemberRepository;

    @Mock
    private DraftNotificationService notificationService;

    private Clock clock;

    @InjectMocks
    private DraftApplicationService service;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(NOW.toInstant(), ZoneOffset.UTC);
        service = new DraftApplicationService(draftRepository, templateRepository, formTemplateRepository,
                approvalGroupRepository, approvalGroupMemberRepository, notificationService, clock);
    }

    @Test
    void givenValidRequest_whenCreate_thenDraftPersisted() {
        ApprovalLineTemplate template = sampleTemplate(ORG);
        DraftFormTemplate formTemplate = sampleFormTemplate(ORG);
        given(templateRepository.findByIdAndActiveTrue(template.getId())).willReturn(Optional.of(template));
        given(formTemplateRepository.findByIdAndActiveTrue(formTemplate.getId())).willReturn(Optional.of(formTemplate));
        given(draftRepository.save(any(Draft.class))).willAnswer(invocation -> invocation.getArgument(0));

        DraftCreateRequest request = new DraftCreateRequest("제목", "내용", "NOTICE",
                template.getId(), formTemplate.getId(), "{}", List.of());
        DraftResponse response = service.createDraft(request, "writer", ORG);

        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.status()).isEqualTo(DraftStatus.DRAFT);
        verify(draftRepository).save(any(Draft.class));
    }

    @Test
    void givenAttachments_whenCreate_thenAttachedToDraft() {
        ApprovalLineTemplate template = sampleTemplate(ORG);
        DraftFormTemplate formTemplate = sampleFormTemplate(ORG);
        given(templateRepository.findByIdAndActiveTrue(template.getId())).willReturn(Optional.of(template));
        given(formTemplateRepository.findByIdAndActiveTrue(formTemplate.getId())).willReturn(Optional.of(formTemplate));
        given(draftRepository.save(any(Draft.class))).willAnswer(invocation -> invocation.getArgument(0));
        DraftAttachmentRequest attachment = new DraftAttachmentRequest(UUID.randomUUID(), "evidence.pdf", "application/pdf", 1234L);

        DraftCreateRequest request = new DraftCreateRequest("제목", "내용", "NOTICE",
                template.getId(), formTemplate.getId(), "{}", List.of(attachment));
        DraftResponse response = service.createDraft(request, "writer", ORG);

        assertThat(response.attachments()).hasSize(1);
        assertThat(response.attachments().get(0).fileName()).isEqualTo("evidence.pdf");
    }

    @Test
    void givenGlobalTemplate_whenCreate_thenOrganizationCheckSkipped() {
        ApprovalLineTemplate template = ApprovalLineTemplate.createGlobal("글로벌", "NOTICE", NOW);
        DraftFormTemplate formTemplate = DraftFormTemplate.create("글로벌", "NOTICE", null, "{}", NOW);
        given(templateRepository.findByIdAndActiveTrue(template.getId())).willReturn(Optional.of(template));
        given(formTemplateRepository.findByIdAndActiveTrue(formTemplate.getId())).willReturn(Optional.of(formTemplate));
        given(draftRepository.save(any(Draft.class))).willAnswer(invocation -> invocation.getArgument(0));

        DraftCreateRequest request = new DraftCreateRequest("제목", "내용", "NOTICE",
                template.getId(), formTemplate.getId(), "{}", List.of());
        DraftResponse response = service.createDraft(request, "writer", ORG);

        assertThat(response.templateCode()).isEqualTo(template.getTemplateCode());
        assertThat(response.organizationCode()).isEqualTo(ORG);
    }

    @Test
    void givenDifferentOrgTemplate_whenCreate_thenThrows() {
        ApprovalLineTemplate template = sampleTemplate("OTHER");
        given(templateRepository.findByIdAndActiveTrue(template.getId())).willReturn(Optional.of(template));

        DraftCreateRequest request = new DraftCreateRequest("제목", "내용", "NOTICE",
                template.getId(), UUID.randomUUID(), "{}", List.of());

        assertThatThrownBy(() -> service.createDraft(request, "writer", ORG))
                .isInstanceOf(DraftAccessDeniedException.class);
    }

    @Test
    void givenDraft_whenSubmitted_thenStatusUpdated() {
        Draft draft = draftReadyForReview();
        given(draftRepository.findById(draft.getId())).willReturn(Optional.of(draft));

        DraftResponse response = service.submitDraft(draft.getId(), "writer", ORG);

        assertThat(response.status()).isEqualTo(DraftStatus.IN_REVIEW);
    }

    @Test
    void givenDraft_whenApproveStep_thenMovesToNextStep() {
        Draft draft = inReviewDraft();
        UUID firstStep = draft.getApprovalSteps().get(0).getId();
        stubGroupMember("GROUP-A", "approver");
        given(draftRepository.findById(draft.getId())).willReturn(Optional.of(draft));

        DraftResponse response = service.approve(draft.getId(), new DraftDecisionRequest(firstStep, "ok"),
                "approver", ORG, false);

        assertThat(response.status()).isEqualTo(DraftStatus.IN_REVIEW);
        assertThat(response.approvalSteps()).anyMatch(step -> step.stepOrder() == 2 && step.state().name().equals("IN_PROGRESS"));
    }

    @Test
    void givenDraft_whenFinalStepApproved_thenCompleted() {
        Draft draft = inReviewDraft();
        given(draftRepository.findById(draft.getId())).willReturn(Optional.of(draft));

        UUID firstStep = draft.getApprovalSteps().get(0).getId();
        stubGroupMember("GROUP-A", "approver1");
        service.approve(draft.getId(), new DraftDecisionRequest(firstStep, "first"), "approver1", ORG, false);

        UUID secondStep = draft.getApprovalSteps().get(1).getId();
        stubGroupMember("GROUP-B", "approver2");
        DraftResponse response = service.approve(draft.getId(), new DraftDecisionRequest(secondStep, "second"),
                "approver2", ORG, false);

        assertThat(response.status()).isEqualTo(DraftStatus.APPROVED);
        assertThat(response.completedAt()).isNotNull();
    }

    @Test
    void givenDraft_whenRejected_thenStatusRejected() {
        Draft draft = inReviewDraft();
        UUID stepId = draft.getApprovalSteps().get(0).getId();
        given(draftRepository.findById(draft.getId())).willReturn(Optional.of(draft));
        stubGroupMember("GROUP-A", "approver");

        DraftResponse response = service.reject(draft.getId(), new DraftDecisionRequest(stepId, "반려"),
                "approver", ORG, false);

        assertThat(response.status()).isEqualTo(DraftStatus.REJECTED);
        assertThat(response.completedAt()).isNotNull();
    }

    @Test
    void givenDraft_whenCancelled_thenStatusCancelled() {
        Draft draft = draftReadyForReview();
        given(draftRepository.findById(draft.getId())).willReturn(Optional.of(draft));

        DraftResponse response = service.cancel(draft.getId(), "writer", ORG);

        assertThat(response.status()).isEqualTo(DraftStatus.CANCELLED);
        assertThat(response.cancelledAt()).isNotNull();
    }

    @Test
    void givenDraftInReview_whenWithdraw_thenStatusWithdrawn() {
        Draft draft = inReviewDraft();
        given(draftRepository.findById(draft.getId())).willReturn(Optional.of(draft));

        DraftResponse response = service.withdraw(draft.getId(), "writer", ORG);

        assertThat(response.status()).isEqualTo(DraftStatus.WITHDRAWN);
        assertThat(response.cancelledAt()).isNull();
        assertThat(response.approvalSteps()).allMatch(step -> step.state().isCompleted());
    }

    @Test
    void givenWithdrawnDraft_whenResubmit_thenRestartFlow() {
        Draft draft = inReviewDraft();
        given(draftRepository.findById(draft.getId())).willReturn(Optional.of(draft));
        service.withdraw(draft.getId(), "writer", ORG);
        given(draftRepository.findById(draft.getId())).willReturn(Optional.of(draft));

        DraftResponse response = service.resubmit(draft.getId(), "writer", ORG);

        assertThat(response.status()).isEqualTo(DraftStatus.IN_REVIEW);
        assertThat(response.submittedAt()).isNotNull();
        assertThat(response.approvalSteps()).anyMatch(step -> step.state().name().equals("IN_PROGRESS"));
    }

    @Test
    void givenInProgressStep_whenDelegate_thenDelegatedToRecorded() {
        Draft draft = inReviewDraft();
        UUID stepId = draft.getApprovalSteps().get(0).getId();
        given(draftRepository.findById(draft.getId())).willReturn(Optional.of(draft));
        stubGroupMember("GROUP-A", "approver");

        DraftResponse response = service.delegate(draft.getId(), new DraftDecisionRequest(stepId, "please handle"),
                "delegatee", "approver", ORG, false);

        assertThat(response.approvalSteps())
                .anyMatch(step -> step.id().equals(stepId) && "delegatee".equals(step.delegatedTo()));
    }

    @Test
    void givenAuditPermission_whenAccessingOtherOrg_thenAllowed() {
        Draft draft = draftReadyForReviewWithOrg("ORG-B");
        given(draftRepository.findById(draft.getId())).willReturn(Optional.of(draft));

        DraftResponse response = service.getDraft(draft.getId(), ORG, true);

        assertThat(response.organizationCode()).isEqualTo("ORG-B");
    }

    @Test
    void givenRowScope_whenListingDrafts_thenDelegatesToRepository() {
        Draft draft = draftReadyForReview();
        Page<Draft> page = new PageImpl<>(List.of(draft));
        given(draftRepository.findAll(any(Specification.class), eq(Pageable.unpaged()))).willReturn(page);

        Page<DraftResponse> result = service.listDrafts(Pageable.unpaged(), RowScope.OWN, ORG, List.of(ORG));

        assertThat(result.getContent()).hasSize(1);
        verify(draftRepository).findAll(any(Specification.class), eq(Pageable.unpaged()));
    }

    private Draft draftReadyForReview() {
        return draftReadyForReviewWithOrg(ORG);
    }

    private Draft draftReadyForReviewWithOrg(String organizationCode) {
        ApprovalLineTemplate template = sampleTemplate(organizationCode);
        Draft draft = Draft.create("제목", "내용", "NOTICE", organizationCode,
                template.getTemplateCode(), "writer", NOW);
        template.instantiateSteps().forEach(draft::addApprovalStep);
        draft.initializeWorkflow(NOW);
        return draft;
    }

    private Draft inReviewDraft() {
        Draft draft = draftReadyForReview();
        draft.submit("writer", NOW.plusMinutes(1));
        return draft;
    }

    private void stubGroupMember(String groupCode, String memberUserId) {
        ApprovalGroup group = ApprovalGroup.create(groupCode, groupCode + " name", null, ORG, null, NOW);
        ApprovalGroupMember member = ApprovalGroupMember.create(memberUserId, ORG, null, NOW);
        given(approvalGroupRepository.findByGroupCode(groupCode)).willReturn(Optional.of(group));
        given(approvalGroupMemberRepository.findByApprovalGroupIdAndActiveTrue(group.getId()))
                .willReturn(List.of(member));
    }

    private ApprovalLineTemplate sampleTemplate(String organizationCode) {
        ApprovalLineTemplate template = ApprovalLineTemplate.create("기본", "NOTICE", organizationCode, NOW);
        template.addStep(1, "GROUP-A", "1차");
        template.addStep(2, "GROUP-B", "2차");
        return template;
    }

    private DraftFormTemplate sampleFormTemplate(String organizationCode) {
        return DraftFormTemplate.create("폼", "NOTICE", organizationCode, "{}", NOW);
    }
}
