package com.example.draft.application;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.draft.application.request.DraftAttachmentRequest;
import com.example.draft.application.request.DraftCreateRequest;
import com.example.draft.application.request.DraftDecisionRequest;
import com.example.draft.application.response.DraftResponse;
import com.example.draft.application.notification.DraftNotificationService;
import com.example.draft.domain.ApprovalGroup;
import com.example.draft.domain.ApprovalLineTemplate;
import com.example.draft.domain.Draft;
import com.example.draft.domain.DraftApprovalStep;
import com.example.draft.domain.DraftAttachment;
import com.example.draft.domain.DraftFormTemplate;
import com.example.draft.domain.exception.DraftAccessDeniedException;
import com.example.draft.domain.exception.DraftNotFoundException;
import com.example.draft.domain.exception.DraftTemplateNotFoundException;
import com.example.draft.domain.repository.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.ApprovalGroupMemberRepository;
import com.example.draft.domain.repository.ApprovalGroupRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;
import com.example.draft.domain.repository.DraftRepository;
import com.example.common.security.RowScope;
import com.example.common.security.RowScopeSpecifications;
import com.example.draft.application.response.DraftTemplateSuggestionResponse;
import com.example.draft.domain.BusinessTemplateMapping;
import com.example.draft.domain.repository.BusinessTemplateMappingRepository;

@Service
public class DraftApplicationService {

    private final DraftRepository draftRepository;
    private final ApprovalLineTemplateRepository templateRepository;
    private final DraftFormTemplateRepository formTemplateRepository;
    private final ApprovalGroupRepository approvalGroupRepository;
    private final ApprovalGroupMemberRepository approvalGroupMemberRepository;
    private final BusinessTemplateMappingRepository mappingRepository;
    private final DraftNotificationService notificationService;
    private final Clock clock;

    public DraftApplicationService(DraftRepository draftRepository,
                                   ApprovalLineTemplateRepository templateRepository,
                                   DraftFormTemplateRepository formTemplateRepository,
                                   ApprovalGroupRepository approvalGroupRepository,
                                   ApprovalGroupMemberRepository approvalGroupMemberRepository,
                                   BusinessTemplateMappingRepository mappingRepository,
                                   DraftNotificationService notificationService,
                                   Clock clock) {
        this.draftRepository = draftRepository;
        this.templateRepository = templateRepository;
        this.formTemplateRepository = formTemplateRepository;
        this.approvalGroupRepository = approvalGroupRepository;
        this.approvalGroupMemberRepository = approvalGroupMemberRepository;
        this.mappingRepository = mappingRepository;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    @Transactional
    public DraftResponse createDraft(DraftCreateRequest request, String actor, String organizationCode) {
        OffsetDateTime now = now();
        TemplateSelection selection = selectTemplates(request, organizationCode);
        ApprovalLineTemplate template = selection.approvalTemplate();
        DraftFormTemplate formTemplate = selection.formTemplate();
        Draft draft = Draft.create(request.title(),
                request.content(),
                request.businessFeatureCode(),
                organizationCode,
                template.getTemplateCode(),
                actor,
                now);
        draft.attachFormTemplate(formTemplate, request.formPayload());
        template.instantiateSteps().forEach(draft::addApprovalStep);
        attachFiles(request.attachments(), draft, actor, now);
        draft.initializeWorkflow(now);
        return DraftResponse.from(draftRepository.save(draft));
    }

    @Transactional
    public DraftResponse submitDraft(UUID draftId, String actor, String organizationCode) {
        Draft draft = loadDraft(draftId);
        draft.assertOrganizationAccess(organizationCode, false);
        draft.submit(actor, now());
        publish("SUBMITTED", draft, actor, null, null, null);
        return DraftResponse.from(draft);
    }

    @Transactional
    public DraftResponse approve(UUID draftId,
                                 DraftDecisionRequest request,
                                 String actor,
                                 String organizationCode,
                                 boolean auditAccess) {
        Draft draft = loadDraft(draftId);
        draft.assertOrganizationAccess(organizationCode, auditAccess);
        ensureStepAccess(draft, actor, organizationCode, request.stepId());
        draft.approveStep(request.stepId(), actor, request.comment(), now());
        publish("APPROVED", draft, actor, request.stepId(), null, request.comment());
        return DraftResponse.from(draft);
    }

    @Transactional
    public DraftResponse reject(UUID draftId,
                                DraftDecisionRequest request,
                                String actor,
                                String organizationCode,
                                boolean auditAccess) {
        Draft draft = loadDraft(draftId);
        draft.assertOrganizationAccess(organizationCode, auditAccess);
        ensureStepAccess(draft, actor, organizationCode, request.stepId());
        draft.rejectStep(request.stepId(), actor, request.comment(), now());
        publish("REJECTED", draft, actor, request.stepId(), null, request.comment());
        return DraftResponse.from(draft);
    }

    @Transactional
    public DraftResponse cancel(UUID draftId, String actor, String organizationCode) {
        Draft draft = loadDraft(draftId);
        draft.assertOrganizationAccess(organizationCode, false);
        draft.cancel(actor, now());
        publish("CANCELLED", draft, actor, null, null, null);
        return DraftResponse.from(draft);
    }

    @Transactional
    public DraftResponse withdraw(UUID draftId, String actor, String organizationCode) {
        Draft draft = loadDraft(draftId);
        draft.assertOrganizationAccess(organizationCode, false);
        draft.withdraw(actor, now());
        publish("WITHDRAWN", draft, actor, null, null, null);
        return DraftResponse.from(draft);
    }

    @Transactional
    public DraftResponse resubmit(UUID draftId, String actor, String organizationCode) {
        Draft draft = loadDraft(draftId);
        draft.assertOrganizationAccess(organizationCode, false);
        draft.resubmit(actor, now());
        publish("RESUBMITTED", draft, actor, null, null, null);
        return DraftResponse.from(draft);
    }

    @Transactional
    public DraftResponse delegate(UUID draftId,
                                  DraftDecisionRequest request,
                                  String delegatedTo,
                                  String actor,
                                  String organizationCode,
                                  boolean auditAccess) {
        Draft draft = loadDraft(draftId);
        draft.assertOrganizationAccess(organizationCode, auditAccess);
        ensureStepAccess(draft, actor, organizationCode, request.stepId());
        draft.delegate(request.stepId(), delegatedTo, actor, request.comment(), now());
        publish("DELEGATED", draft, actor, request.stepId(), delegatedTo, request.comment());
        return DraftResponse.from(draft);
    }

    @Transactional(readOnly = true)
    public DraftResponse getDraft(UUID draftId, String organizationCode, boolean auditAccess) {
        Draft draft = loadDraft(draftId);
        draft.assertOrganizationAccess(organizationCode, auditAccess);
        return DraftResponse.from(draft);
    }

    @Transactional(readOnly = true)
    public Page<DraftResponse> listDrafts(Pageable pageable,
                                          RowScope rowScope,
                                          String organizationCode,
                                          Collection<String> scopedOrganizations) {
        if (rowScope == RowScope.CUSTOM) {
            throw new UnsupportedOperationException("CUSTOM RowScope는 별도 전략이 필요합니다.");
        }
        Specification<Draft> specification = RowScopeSpecifications.organizationScoped(
                "organizationCode",
                rowScope,
                organizationCode,
                scopedOrganizations
        );
        return draftRepository.findAll(specification, pageable)
                .map(DraftResponse::from);
    }

    @Transactional(readOnly = true)
    public DraftTemplateSuggestionResponse suggestTemplate(String businessFeatureCode, String organizationCode) {
        BusinessTemplateMapping mapping = mappingRepository.findByBusinessFeatureCodeAndOrganizationCodeAndActiveTrue(businessFeatureCode, organizationCode)
                .or(() -> mappingRepository.findByBusinessFeatureCodeAndOrganizationCodeIsNullAndActiveTrue(businessFeatureCode))
                .orElseThrow(() -> new DraftTemplateNotFoundException("기본 매핑된 템플릿이 없습니다."));
        return DraftTemplateSuggestionResponse.from(mapping);
    }

    private Draft loadDraft(UUID id) {
        return draftRepository.findById(id)
                .orElseThrow(() -> new DraftNotFoundException("기안을 찾을 수 없습니다."));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    private void attachFiles(List<DraftAttachmentRequest> requests, Draft draft, String actor, OffsetDateTime now) {
        requests.forEach(attachment -> {
            DraftAttachment entity = DraftAttachment.create(
                    attachment.fileId(),
                    attachment.fileName(),
                    attachment.contentType(),
                    attachment.fileSize(),
                    actor,
                    now
            );
            draft.addAttachment(entity);
        });
    }

    private void ensureStepAccess(Draft draft, String actor, String organizationCode, UUID stepId) {
        DraftApprovalStep step = draft.findStep(stepId);
        ApprovalGroup group = approvalGroupRepository.findByGroupCode(step.getApprovalGroupCode())
                .orElseThrow(() -> new DraftNotFoundException("결재 그룹을 찾을 수 없습니다."));
        boolean permitted = approvalGroupMemberRepository.findByApprovalGroupIdAndActiveTrue(group.getId())
                .stream()
                .anyMatch(member -> member.getMemberUserId().equals(actor)
                        && (member.getMemberOrgCode() == null || member.getMemberOrgCode().equals(organizationCode)));
        if (!permitted) {
            throw new DraftAccessDeniedException("결재 권한이 없습니다.");
        }
    }

    private void publish(String action, Draft draft, String actor, UUID stepId, String delegatedTo, String comment) {
        notificationService.notify(action, draft, actor, stepId, delegatedTo, comment, now());
    }

    private TemplateSelection selectTemplates(DraftCreateRequest request, String organizationCode) {
        if (request.templateId() != null && request.formTemplateId() != null) {
            ApprovalLineTemplate template = templateRepository.findByIdAndActiveTrue(request.templateId())
                    .orElseThrow(() -> new DraftTemplateNotFoundException("결재선 템플릿을 찾을 수 없습니다."));
            template.assertOrganization(organizationCode);
            DraftFormTemplate formTemplate = formTemplateRepository.findByIdAndActiveTrue(request.formTemplateId())
                    .orElseThrow(() -> new DraftTemplateNotFoundException("기안 양식을 찾을 수 없습니다."));
            formTemplate.assertOrganization(organizationCode);
            if (!formTemplate.matchesBusiness(request.businessFeatureCode())) {
                throw new DraftTemplateNotFoundException("비즈니스 유형에 맞는 기안 양식이 아닙니다.");
            }
            return new TemplateSelection(template, formTemplate);
        }
        BusinessTemplateMapping mapping = mappingRepository.findByBusinessFeatureCodeAndOrganizationCodeAndActiveTrue(request.businessFeatureCode(), organizationCode)
                .or(() -> mappingRepository.findByBusinessFeatureCodeAndOrganizationCodeIsNullAndActiveTrue(request.businessFeatureCode()))
                .orElseThrow(() -> new DraftTemplateNotFoundException("기본 매핑된 템플릿을 찾을 수 없습니다."));
        mapping.getDraftFormTemplate().assertOrganization(organizationCode);
        mapping.getApprovalLineTemplate().assertOrganization(organizationCode);
        return new TemplateSelection(mapping.getApprovalLineTemplate(), mapping.getDraftFormTemplate());
    }

    private record TemplateSelection(ApprovalLineTemplate approvalTemplate, DraftFormTemplate formTemplate) {
    }
}
