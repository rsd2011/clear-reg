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
import com.example.draft.application.response.DraftHistoryResponse;
import com.example.draft.application.response.DraftReferenceResponse;
import com.example.draft.application.audit.DraftAuditEvent;
import com.example.draft.application.audit.DraftAuditPublisher;
import com.example.draft.domain.ApprovalGroup;
import com.example.draft.domain.ApprovalLineTemplate;
import com.example.draft.domain.Draft;
import com.example.draft.domain.DraftApprovalStep;
import com.example.draft.domain.DraftAttachment;
import com.example.draft.domain.DraftFormTemplate;
import com.example.draft.domain.exception.DraftAccessDeniedException;
import com.example.draft.domain.exception.DraftNotFoundException;
import com.example.draft.domain.exception.DraftTemplateNotFoundException;
import com.example.draft.domain.DraftAction;
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
import com.example.draft.domain.repository.DraftHistoryRepository;
import com.example.draft.domain.repository.DraftReferenceRepository;
import com.example.draft.domain.DraftStatus;

@Service
public class DraftApplicationService {

    private final DraftRepository draftRepository;
    private final ApprovalLineTemplateRepository templateRepository;
    private final DraftFormTemplateRepository formTemplateRepository;
    private final ApprovalGroupRepository approvalGroupRepository;
    private final ApprovalGroupMemberRepository approvalGroupMemberRepository;
    private final BusinessTemplateMappingRepository mappingRepository;
    private final DraftHistoryRepository draftHistoryRepository;
    private final DraftReferenceRepository draftReferenceRepository;
    private final DraftNotificationService notificationService;
    private final DraftAuditPublisher auditPublisher;
    private final com.example.draft.application.business.DraftBusinessPolicy businessPolicy;
    private final Clock clock;

    public DraftApplicationService(DraftRepository draftRepository,
                                   ApprovalLineTemplateRepository templateRepository,
                                   DraftFormTemplateRepository formTemplateRepository,
                                   ApprovalGroupRepository approvalGroupRepository,
                                   ApprovalGroupMemberRepository approvalGroupMemberRepository,
                                   BusinessTemplateMappingRepository mappingRepository,
                                   DraftHistoryRepository draftHistoryRepository,
                                   DraftReferenceRepository draftReferenceRepository,
                                   DraftNotificationService notificationService,
                                   DraftAuditPublisher auditPublisher,
                                   com.example.draft.application.business.DraftBusinessPolicy businessPolicy,
                                   Clock clock) {
        this.draftRepository = draftRepository;
        this.templateRepository = templateRepository;
        this.formTemplateRepository = formTemplateRepository;
        this.approvalGroupRepository = approvalGroupRepository;
        this.approvalGroupMemberRepository = approvalGroupMemberRepository;
        this.mappingRepository = mappingRepository;
        this.draftHistoryRepository = draftHistoryRepository;
        this.draftReferenceRepository = draftReferenceRepository;
        this.notificationService = notificationService;
        this.auditPublisher = auditPublisher;
        this.businessPolicy = businessPolicy;
        this.clock = clock;
    }

    @Transactional
    public DraftResponse createDraft(DraftCreateRequest request, String actor, String organizationCode) {
        OffsetDateTime now = now();
        businessPolicy.assertCreatable(request.businessFeatureCode(), organizationCode, actor);
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
        Draft saved = draftRepository.save(draft);
        businessPolicy.afterStateChanged(saved.getId(), saved.getBusinessFeatureCode(), saved.getStatus(), DraftAction.CREATED, actor);
        return DraftResponse.from(saved);
    }

    @Transactional
    public DraftResponse submitDraft(UUID draftId, String actor, String organizationCode) {
        Draft draft = loadDraft(draftId);
        draft.assertOrganizationAccess(organizationCode, false);
        draft.submit(actor, now());
        publish(DraftAction.SUBMITTED, draft, actor, null, null, null);
        audit(DraftAction.SUBMITTED, draft, actor, null, organizationCode, null, null);
        businessPolicy.afterStateChanged(draft.getId(), draft.getBusinessFeatureCode(), draft.getStatus(), DraftAction.SUBMITTED, actor);
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
        publish(DraftAction.APPROVED, draft, actor, request.stepId(), null, request.comment());
        audit(DraftAction.APPROVED, draft, actor, request.comment(), organizationCode, null, null);
        businessPolicy.afterStateChanged(draft.getId(), draft.getBusinessFeatureCode(), draft.getStatus(), DraftAction.APPROVED, actor);
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
        publish(DraftAction.REJECTED, draft, actor, request.stepId(), null, request.comment());
        audit(DraftAction.REJECTED, draft, actor, request.comment(), organizationCode, null, null);
        businessPolicy.afterStateChanged(draft.getId(), draft.getBusinessFeatureCode(), draft.getStatus(), DraftAction.REJECTED, actor);
        return DraftResponse.from(draft);
    }

    @Transactional
    public DraftResponse cancel(UUID draftId, String actor, String organizationCode) {
        Draft draft = loadDraft(draftId);
        draft.assertOrganizationAccess(organizationCode, false);
        draft.cancel(actor, now());
        publish(DraftAction.CANCELLED, draft, actor, null, null, null);
        audit(DraftAction.CANCELLED, draft, actor, null, organizationCode, null, null);
        businessPolicy.afterStateChanged(draft.getId(), draft.getBusinessFeatureCode(), draft.getStatus(), DraftAction.CANCELLED, actor);
        return DraftResponse.from(draft);
    }

    @Transactional
    public DraftResponse withdraw(UUID draftId, String actor, String organizationCode) {
        Draft draft = loadDraft(draftId);
        draft.assertOrganizationAccess(organizationCode, false);
        draft.withdraw(actor, now());
        publish(DraftAction.WITHDRAWN, draft, actor, null, null, null);
        audit(DraftAction.WITHDRAWN, draft, actor, null, organizationCode, null, null);
        businessPolicy.afterStateChanged(draft.getId(), draft.getBusinessFeatureCode(), draft.getStatus(), DraftAction.WITHDRAWN, actor);
        return DraftResponse.from(draft);
    }

    @Transactional
    public DraftResponse resubmit(UUID draftId, String actor, String organizationCode) {
        Draft draft = loadDraft(draftId);
        draft.assertOrganizationAccess(organizationCode, false);
        draft.resubmit(actor, now());
        publish(DraftAction.RESUBMITTED, draft, actor, null, null, null);
        audit(DraftAction.RESUBMITTED, draft, actor, null, organizationCode, null, null);
        businessPolicy.afterStateChanged(draft.getId(), draft.getBusinessFeatureCode(), draft.getStatus(), DraftAction.RESUBMITTED, actor);
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
        publish(DraftAction.DELEGATED, draft, actor, request.stepId(), delegatedTo, request.comment());
        audit(DraftAction.DELEGATED, draft, actor, request.comment(), organizationCode, null, null);
        businessPolicy.afterStateChanged(draft.getId(), draft.getBusinessFeatureCode(), draft.getStatus(), DraftAction.DELEGATED, actor);
        return DraftResponse.from(draft);
    }

    @Transactional(readOnly = true)
    public DraftResponse getDraft(UUID draftId, String organizationCode, String requester, boolean auditAccess) {
        Draft draft = loadDraft(draftId);
        draft.assertOrganizationAccess(organizationCode, auditAccess);
        enforceReadAccess(draft, requester, auditAccess);
        return DraftResponse.from(draft);
    }

    @Transactional(readOnly = true)
    public Page<DraftResponse> listDrafts(Pageable pageable,
                                          RowScope rowScope,
                                          String organizationCode,
                                          Collection<String> scopedOrganizations,
                                          String status,
                                          String businessFeatureCode,
                                          String createdBy,
                                          String titleContains) {
        if (rowScope == RowScope.CUSTOM) {
            throw new UnsupportedOperationException("CUSTOM RowScope는 별도 전략이 필요합니다.");
        }
        rowScope = normalizeRowScope(rowScope);
        Specification<Draft> specification = RowScopeSpecifications.<Draft>organizationScoped(
                "organizationCode",
                rowScope,
                organizationCode,
                scopedOrganizations
        ).and(filter(status, businessFeatureCode, createdBy, titleContains));
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

    @Transactional(readOnly = true)
    public List<DraftHistoryResponse> listHistory(UUID draftId, String organizationCode, String requester, boolean auditAccess) {
        Draft draft = loadDraft(draftId);
        draft.assertOrganizationAccess(organizationCode, auditAccess);
        enforceReadAccess(draft, requester, auditAccess);
        return draftHistoryRepository.findByDraftIdOrderByOccurredAtAsc(draftId)
                .stream()
                .map(DraftHistoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DraftReferenceResponse> listReferences(UUID draftId, String organizationCode, String requester, boolean auditAccess) {
        Draft draft = loadDraft(draftId);
        draft.assertOrganizationAccess(organizationCode, auditAccess);
        enforceReadAccess(draft, requester, auditAccess);
        return draftReferenceRepository.findByDraftIdAndActiveTrue(draftId)
                .stream()
                .map(DraftReferenceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DraftHistoryResponse> listAudit(UUID draftId,
                                                String organizationCode,
                                                String requester,
                                                boolean auditAccess,
                                                String action,
                                                String actor,
                                                OffsetDateTime from,
                                                OffsetDateTime to) {
        Draft draft = loadDraft(draftId);
        draft.assertOrganizationAccess(organizationCode, auditAccess);
        enforceReadAccess(draft, requester, auditAccess);
        return draftHistoryRepository.findByDraftIdOrderByOccurredAtAsc(draftId).stream()
                .filter(h -> h.getEventType().startsWith("AUDIT:"))
                .filter(h -> action == null || h.getEventType().equals("AUDIT:" + action))
                .filter(h -> actor == null || actor.equals(h.getActor()))
                .filter(h -> from == null || !h.getOccurredAt().isBefore(from))
                .filter(h -> to == null || !h.getOccurredAt().isAfter(to))
                .map(DraftHistoryResponse::from)
                .toList();
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

    private void publish(DraftAction action, Draft draft, String actor, UUID stepId, String delegatedTo, String comment) {
        notificationService.notify(action.name(), draft, actor, stepId, delegatedTo, comment, now());
    }

    private void audit(DraftAction action, Draft draft, String actor, String comment, String organizationCode, String ip, String userAgent) {
        String details = comment != null ? comment : "%s by %s".formatted(action.name(), actor);
        if (ip == null || userAgent == null) {
            var ctx = com.example.draft.application.audit.AuditRequestContextHolder.current();
            ip = ip == null ? ctx.map(com.example.draft.application.audit.AuditRequestContext::ip).orElse(null) : ip;
            userAgent = userAgent == null ? ctx.map(com.example.draft.application.audit.AuditRequestContext::userAgent).orElse(null) : userAgent;
        }
        OffsetDateTime occurredAt = now();
        draftHistoryRepository.save(
                com.example.draft.domain.DraftHistory.entry(draft, "AUDIT:" + action.name(), actor, details, occurredAt));
        auditPublisher.publish(new DraftAuditEvent(action, draft.getId(), actor, organizationCode, comment, ip, userAgent, occurredAt));
    }

    /**
     * 열람 허용 주체: 작성자, 결재선 참여자(멤버/위임 포함), 참조자, 감사(AUDIT) 권한 보유자.
     */
    private void enforceReadAccess(Draft draft, String requesterUsername, boolean auditAccess) {
        if (auditAccess) {
            return;
        }
        boolean allowed = draft.getCreatedBy().equals(requesterUsername)
                || draft.getApprovalSteps().stream().anyMatch(step -> {
                    if (step.getActedBy() != null && step.getActedBy().equals(requesterUsername)) return true;
                    return step.getDelegatedTo() != null && step.getDelegatedTo().equals(requesterUsername);
                })
                || draftReferenceRepository.findByDraftIdAndActiveTrue(draft.getId()).stream()
                .anyMatch(ref -> ref.getReferencedUserId().equals(requesterUsername));
        if (!allowed) {
            throw new DraftAccessDeniedException("열람 권한이 없습니다.");
        }
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

    private RowScope normalizeRowScope(RowScope rowScope) {
        if (rowScope == null) {
            return RowScope.ORG;
        }
        if (rowScope == RowScope.OWN) {
            return RowScope.ORG;
        }
        return rowScope;
    }

    private Specification<Draft> filter(String status, String businessFeature, String createdBy, String titleContains) {
        Specification<Draft> spec = Specification.where(null);
        if (status != null) {
            DraftStatus draftStatus = DraftStatus.valueOf(status);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), draftStatus));
        }
        if (businessFeature != null && !businessFeature.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("businessFeatureCode")), businessFeature.toLowerCase()));
        }
        if (createdBy != null && !createdBy.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("createdBy"), createdBy));
        }
        if (titleContains != null && !titleContains.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("title")), "%" + titleContains.toLowerCase() + "%"));
        }
        return spec;
    }
}
