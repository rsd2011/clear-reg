package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.admin.approval.domain.ApprovalGroup;
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

import com.example.draft.application.dto.DraftAttachmentRequest;
import com.example.draft.application.dto.DraftCreateRequest;
import com.example.draft.application.dto.DraftDecisionRequest;
import com.example.draft.application.dto.DraftResponse;
import com.example.admin.approval.domain.ApprovalLineTemplate;
import com.example.draft.domain.DraftApprovalStep;
import com.example.draft.domain.Draft;
import com.example.draft.domain.DraftFormTemplate;
import com.example.draft.domain.DraftStatus;
import com.example.draft.domain.DraftTemplatePreset;
import com.example.draft.domain.exception.DraftTemplateNotFoundException;
import com.example.approval.api.ApprovalFacade;
import com.example.approval.api.ApprovalStatus;
import com.example.approval.api.ApprovalStatusSnapshot;
import com.example.admin.approval.repository.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.BusinessTemplateMappingRepository;
import com.example.draft.domain.repository.DraftHistoryRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;
import com.example.draft.domain.repository.DraftReferenceRepository;
import com.example.draft.domain.repository.DraftRepository;
import com.example.draft.domain.repository.DraftTemplatePresetRepository;
import com.example.draft.application.notification.DraftNotificationService;
import com.example.common.security.RowScope;
import com.example.draft.application.business.DraftBusinessPolicy;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class DraftApplicationServiceTest {

    private static final String ORG = "ORG-001";
    private static final String ACTOR = "user-1";
    private static final OffsetDateTime NOW = OffsetDateTime.ofInstant(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private DraftRepository draftRepository;

    @Mock
    private ApprovalLineTemplateRepository templateRepository;

    @Mock
    private DraftFormTemplateRepository formTemplateRepository;

    @Mock
    private DraftNotificationService notificationService;

    @Mock
    private BusinessTemplateMappingRepository mappingRepository;

    @Mock
    private DraftHistoryRepository draftHistoryRepository;

    @Mock
    private DraftReferenceRepository draftReferenceRepository;

    @Mock
    private DraftTemplatePresetRepository presetRepository;

    @Mock
    private com.example.draft.application.audit.DraftAuditPublisher auditPublisher;

    @Mock
    private DraftBusinessPolicy businessPolicy;

    @Mock
    private ApprovalFacade approvalFacade;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private ObjectMapper objectMapper;
    private Clock clock;

    @InjectMocks
    private DraftApplicationService service;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(NOW.toInstant(), ZoneOffset.UTC);
        objectMapper = new ObjectMapper();
        service = new DraftApplicationService(draftRepository, templateRepository, formTemplateRepository,
                mappingRepository,
                draftHistoryRepository, draftReferenceRepository, presetRepository, notificationService, auditPublisher, businessPolicy,
                approvalFacade, eventPublisher, objectMapper, clock);
        lenient().when(approvalFacade.requestApproval(any())).thenAnswer(invocation -> {
            var cmd = invocation.getArgument(0, com.example.approval.api.dto.ApprovalRequestCommand.class);
            return new ApprovalStatusSnapshot(UUID.randomUUID(), cmd.draftId(), ApprovalStatus.REQUESTED, List.of());
        });
    }

    @Test
    void givenValidRequest_whenCreate_thenDraftPersisted() {
        ApprovalLineTemplate template = sampleTemplate(ORG);
        DraftFormTemplate formTemplate = sampleFormTemplate(ORG);
        given(templateRepository.findByIdAndActiveTrue(template.getId())).willReturn(Optional.of(template));
        given(formTemplateRepository.findByIdAndActiveTrue(formTemplate.getId())).willReturn(Optional.of(formTemplate));
        given(draftRepository.save(any(Draft.class))).willAnswer(invocation -> invocation.getArgument(0));

        DraftCreateRequest request = new DraftCreateRequest("제목", "내용", "NOTICE",
                template.getId(), formTemplate.getId(), "{}", List.of(), null, java.util.Map.of());
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
                template.getId(), formTemplate.getId(), "{}", List.of(attachment), null, java.util.Map.of());
        DraftResponse response = service.createDraft(request, "writer", ORG);

        assertThat(response.attachments()).hasSize(1);
        assertThat(response.attachments().get(0).fileName()).isEqualTo("evidence.pdf");
    }

    @Test
    void givenGlobalTemplate_whenCreate_thenOrganizationCheckSkipped() {
        ApprovalLineTemplate template = ApprovalLineTemplate.create("글로벌", 0, null, NOW);
        DraftFormTemplate formTemplate = DraftFormTemplate.create("글로벌", "NOTICE", null, "{}", NOW);
        given(templateRepository.findByIdAndActiveTrue(template.getId())).willReturn(Optional.of(template));
        given(formTemplateRepository.findByIdAndActiveTrue(formTemplate.getId())).willReturn(Optional.of(formTemplate));
        given(draftRepository.save(any(Draft.class))).willAnswer(invocation -> invocation.getArgument(0));

        DraftCreateRequest request = new DraftCreateRequest("제목", "내용", "NOTICE",
                template.getId(), formTemplate.getId(), "{}", List.of(), null, java.util.Map.of());
        DraftResponse response = service.createDraft(request, "writer", ORG);

        assertThat(response.templateCode()).isEqualTo(template.getTemplateCode());
    }

    @Test
    void givenNullTemplateIds_whenCreate_thenUsesDefaultMapping() {
        ApprovalLineTemplate template = sampleTemplate(ORG);
        DraftFormTemplate formTemplate = sampleFormTemplate(ORG);
        com.example.draft.domain.BusinessTemplateMapping mapping = com.example.draft.domain.BusinessTemplateMapping.create(
                "NOTICE", ORG, template, formTemplate, NOW);
        given(mappingRepository.findByBusinessFeatureCodeAndOrganizationCodeAndActiveTrue("NOTICE", ORG))
                .willReturn(Optional.of(mapping));
        given(draftRepository.save(any(Draft.class))).willAnswer(invocation -> invocation.getArgument(0));

        DraftCreateRequest request = new DraftCreateRequest("제목", "내용", "NOTICE",
                null, null, "{}", List.of(), null, java.util.Map.of());

        DraftResponse response = service.createDraft(request, "writer", ORG);

        assertThat(response.templateCode()).isEqualTo(template.getTemplateCode());
        assertThat(response.formTemplateCode()).isEqualTo(formTemplate.getTemplateCode());
    }

    @Test
    void givenTemplatePreset_whenCreate_thenUsesPresetDefaultsAndVariables() {
        ApprovalLineTemplate template = sampleTemplate(ORG);
        DraftFormTemplate formTemplate = sampleFormTemplate(ORG);
        String defaultPayload = objectMapper.createObjectNode()
                .put("base", true)
                .put("field", "default")
                .toString();
        DraftTemplatePreset preset = DraftTemplatePreset.create(
                "사전 기안",
                "NOTICE",
                ORG,
                "{작성자}의 휴가",
                "본문 {custom}",
                formTemplate,
                template,
                defaultPayload,
                "[\"custom\",\"작성자\"]",
                true,
                NOW);
        given(presetRepository.findByIdAndActiveTrue(preset.getId())).willReturn(Optional.of(preset));
        given(draftRepository.save(any(Draft.class))).willAnswer(invocation -> invocation.getArgument(0));

        DraftCreateRequest request = new DraftCreateRequest("ignored", "ignored", "NOTICE",
                null, null, "{\"field\":\"user\"}", List.of(), preset.getId(), java.util.Map.of("custom", "급히"));

        DraftResponse response = service.createDraft(request, "writer", ORG);

        assertThat(response.templatePresetId()).isEqualTo(preset.getId());
        assertThat(response.title()).contains("writer");
        assertThat(response.content()).contains("급히");
        assertThat(response.formPayload()).contains("base").contains("user");
    }

    @Test
    void givenPresetWithAllowedVariables_whenCreate_thenDisallowsUnknownVariableAndMergesPayload() {
        ApprovalLineTemplate approvalTemplate = sampleTemplate(ORG);
        DraftFormTemplate formTemplate = sampleFormTemplate(ORG);
        DraftTemplatePreset preset = DraftTemplatePreset.create(
                "프리셋",
                "NOTICE",
                ORG,
                "{custom} {작성자}",
                "내용 {custom} {other}",
                formTemplate,
                null,
                "{\"nested\":{\"x\":1}}",
                "[\"custom\"]",
                true,
                NOW);
        com.example.draft.domain.BusinessTemplateMapping mapping = com.example.draft.domain.BusinessTemplateMapping.create(
                "NOTICE", ORG, approvalTemplate, formTemplate, NOW);

        given(presetRepository.findByIdAndActiveTrue(preset.getId())).willReturn(Optional.of(preset));
        given(mappingRepository.findByBusinessFeatureCodeAndOrganizationCodeAndActiveTrue("NOTICE", ORG))
                .willReturn(Optional.of(mapping));
        given(draftRepository.save(any(Draft.class))).willAnswer(invocation -> invocation.getArgument(0));

        DraftCreateRequest request = new DraftCreateRequest("원제목", "원본문", "NOTICE",
                null, null, "{\"nested\":{\"y\":2},\"b\":3}", List.of(), preset.getId(),
                java.util.Map.of("custom", "C", "other", "IGNORED"));

        DraftResponse response = service.createDraft(request, "actor", ORG);

        assertThat(response.title()).contains("C").contains("actor");
        assertThat(response.content()).contains("C").doesNotContain("IGNORED");
        assertThat(response.formPayload()).contains("\"x\":1").contains("\"y\":2").contains("\"b\":3");
    }

    @Test
    void givenGlobalFallback_whenSuggestTemplate_thenReturnsGlobalMapping() {
        ApprovalLineTemplate template = sampleTemplate("GLOBAL");
        DraftFormTemplate formTemplate = sampleFormTemplate("GLOBAL");
        com.example.draft.domain.BusinessTemplateMapping global = com.example.draft.domain.BusinessTemplateMapping.create(
                "NOTICE", null, template, formTemplate, NOW);
        given(mappingRepository.findByBusinessFeatureCodeAndOrganizationCodeAndActiveTrue("NOTICE", ORG))
                .willReturn(Optional.empty());
        given(mappingRepository.findByBusinessFeatureCodeAndOrganizationCodeIsNullAndActiveTrue("NOTICE"))
                .willReturn(Optional.of(global));

        var suggestion = service.suggestTemplate("NOTICE", ORG);

        assertThat(suggestion.approvalTemplateId()).isEqualTo(template.getId());
        assertThat(suggestion.formTemplateId()).isEqualTo(formTemplate.getId());
    }

    @Test
    void givenNoMapping_whenSuggestTemplate_thenThrows() {
        given(mappingRepository.findByBusinessFeatureCodeAndOrganizationCodeAndActiveTrue("NOTICE", ORG))
                .willReturn(Optional.empty());
        given(mappingRepository.findByBusinessFeatureCodeAndOrganizationCodeIsNullAndActiveTrue("NOTICE"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.suggestTemplate("NOTICE", ORG))
                .isInstanceOf(DraftTemplateNotFoundException.class);
    }

    @Test
    void givenDraft_whenSubmitted_thenStatusUpdated() {
        Draft draft = draftReadyForReview();
        given(draftRepository.findById(draft.getId())).willReturn(Optional.of(draft));

        DraftResponse response = service.submitDraft(draft.getId(), "writer", ORG);

        assertThat(response.status()).isEqualTo(DraftStatus.IN_REVIEW);
        verify(notificationService).notify(eq("SUBMITTED"), any(Draft.class), eq("writer"),
                isNull(), isNull(), isNull(), any());
        verify(approvalFacade).requestApproval(any());
        assertThat(draft.getApprovalRequestId()).isNotNull();
    }

    @Test
    void givenDraft_whenApproveStep_thenMovesToNextStep() {
        Draft draft = inReviewDraft();
        UUID firstStep = draft.getApprovalSteps().get(0).getId();
        given(draftRepository.findById(draft.getId())).willReturn(Optional.of(draft));

        DraftResponse response = service.approve(draft.getId(), new DraftDecisionRequest(firstStep, "ok"),
                "approver", ORG, false);

        assertThat(response.status()).isEqualTo(DraftStatus.IN_REVIEW);
        assertThat(response.approvalSteps()).anyMatch(step -> step.stepOrder() == 2 && step.state().name().equals("IN_PROGRESS"));
        verify(notificationService).notify(eq("APPROVED"), any(Draft.class), eq("approver"),
                eq(firstStep), isNull(), eq("ok"), any());
    }

    @Test
    void givenDraft_whenFinalStepApproved_thenCompleted() {
        Draft draft = inReviewDraft();
        given(draftRepository.findById(draft.getId())).willReturn(Optional.of(draft));

        UUID firstStep = draft.getApprovalSteps().get(0).getId();
        service.approve(draft.getId(), new DraftDecisionRequest(firstStep, "first"), "approver1", ORG, false);

        UUID secondStep = draft.getApprovalSteps().get(1).getId();
        DraftResponse response = service.approve(draft.getId(), new DraftDecisionRequest(secondStep, "second"),
                "approver2", ORG, false);

        assertThat(response.status()).isEqualTo(DraftStatus.APPROVED);
        assertThat(response.completedAt()).isNotNull();
        verify(notificationService).notify(eq("APPROVED"), any(Draft.class), eq("approver2"),
                eq(secondStep), isNull(), eq("second"), any());
    }

    @Test
    void givenDraft_whenRejected_thenStatusRejected() {
        Draft draft = inReviewDraft();
        UUID stepId = draft.getApprovalSteps().get(0).getId();
        given(draftRepository.findById(draft.getId())).willReturn(Optional.of(draft));

        DraftResponse response = service.reject(draft.getId(), new DraftDecisionRequest(stepId, "반려"),
                "approver", ORG, false);

        assertThat(response.status()).isEqualTo(DraftStatus.REJECTED);
        assertThat(response.completedAt()).isNotNull();
        verify(notificationService).notify(eq("REJECTED"), any(Draft.class), eq("approver"),
                eq(stepId), isNull(), eq("반려"), any());
    }

    @Test
    void givenDraft_whenCancelled_thenStatusCancelled() {
        Draft draft = draftReadyForReview();
        given(draftRepository.findById(draft.getId())).willReturn(Optional.of(draft));

        DraftResponse response = service.cancel(draft.getId(), "writer", ORG);

        assertThat(response.status()).isEqualTo(DraftStatus.CANCELLED);
        assertThat(response.cancelledAt()).isNotNull();
        verify(notificationService).notify(eq("CANCELLED"), any(Draft.class), eq("writer"),
                isNull(), isNull(), isNull(), any());
    }

    @Test
    void givenDraftInReview_whenWithdraw_thenStatusWithdrawn() {
        Draft draft = inReviewDraft();
        given(draftRepository.findById(draft.getId())).willReturn(Optional.of(draft));

        DraftResponse response = service.withdraw(draft.getId(), "writer", ORG);

        assertThat(response.status()).isEqualTo(DraftStatus.WITHDRAWN);
        assertThat(response.cancelledAt()).isNull();
        assertThat(response.approvalSteps()).allMatch(step -> step.state().isCompleted());
        verify(notificationService).notify(eq("WITHDRAWN"), any(Draft.class), eq("writer"),
                isNull(), isNull(), isNull(), any());
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
        verify(notificationService).notify(eq("RESUBMITTED"), any(Draft.class), eq("writer"),
                isNull(), isNull(), isNull(), any());
    }

    @Test
    void givenInProgressStep_whenDelegate_thenDelegatedToRecorded() {
        Draft draft = inReviewDraft();
        UUID stepId = draft.getApprovalSteps().get(0).getId();
        given(draftRepository.findById(draft.getId())).willReturn(Optional.of(draft));

        DraftResponse response = service.delegate(draft.getId(), new DraftDecisionRequest(stepId, "please handle"),
                "delegatee", "approver", ORG, false);

        assertThat(response.approvalSteps())
                .anyMatch(step -> step.id().equals(stepId) && "delegatee".equals(step.delegatedTo()));
        verify(notificationService).notify(eq("DELEGATED"), any(Draft.class), eq("approver"),
                eq(stepId), eq("delegatee"), eq("please handle"), any());
    }

    @Test
    void givenAuditPermission_whenAccessingOtherOrg_thenAllowed() {
        Draft draft = draftReadyForReviewWithOrg("ORG-B");
        given(draftRepository.findById(draft.getId())).willReturn(Optional.of(draft));

        DraftResponse response = service.getDraft(draft.getId(), ORG, ACTOR, true);

    }

    @Test
    void givenRowScope_whenListingDrafts_thenDelegatesToRepository() {
        Draft draft = draftReadyForReview();
        Page<Draft> page = new PageImpl<>(List.of(draft));
        given(draftRepository.findAll(any(Specification.class), eq(Pageable.unpaged()))).willReturn(page);

        Page<DraftResponse> result = service.listDrafts(Pageable.unpaged(), RowScope.OWN, ORG, List.of(ORG),
                null, null, null, null);

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
        template.getSteps().stream()
                .map(DraftApprovalStep::fromTemplate)
                .forEach(draft::addApprovalStep);
        draft.initializeWorkflow(NOW);
        return draft;
    }

    private Draft inReviewDraft() {
        Draft draft = draftReadyForReview();
        draft.submit("writer", NOW.plusMinutes(1));
        return draft;
    }

    private ApprovalLineTemplate sampleTemplate(String organizationCode) {
        ApprovalLineTemplate template = ApprovalLineTemplate.create("기본", 0, null, NOW);
        // ApprovalGroup을 생성하고 addStep 호출
        ApprovalGroup group1 = ApprovalGroup.create("GRP1", "그룹1", "설명", 1, NOW);
        ApprovalGroup group2 = ApprovalGroup.create("GRP2", "그룹2", "설명", 2, NOW);
        template.addStep(1, group1);
        template.addStep(2, group2);
        return template;
    }

    private DraftFormTemplate sampleFormTemplate(String organizationCode) {
        return DraftFormTemplate.create("폼", "NOTICE", organizationCode, "{}", NOW);
    }
}
