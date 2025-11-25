package com.example.draft.application;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.example.approval.api.ApprovalFacade;
import com.example.approval.api.ApprovalRequestCommand;
import com.example.approval.api.ApprovalStatusSnapshot;
import com.example.approval.api.event.DraftSubmittedEvent;
import com.example.approval.domain.ApprovalGroup;
import com.example.approval.domain.ApprovalLineTemplate;
import com.example.draft.domain.Draft;
import com.example.draft.domain.DraftApprovalStep;
import com.example.draft.domain.DraftAttachment;
import com.example.draft.domain.DraftFormTemplate;
import com.example.draft.domain.exception.DraftAccessDeniedException;
import com.example.draft.domain.exception.DraftNotFoundException;
import com.example.draft.domain.exception.DraftTemplateNotFoundException;
import com.example.draft.domain.DraftAction;
import com.example.approval.domain.repository.ApprovalLineTemplateRepository;
import com.example.approval.domain.repository.ApprovalGroupMemberRepository;
import com.example.approval.domain.repository.ApprovalGroupRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;
import com.example.draft.domain.repository.DraftRepository;
import com.example.common.security.RowScope;
import com.example.common.security.RowScopeSpecifications;
import com.example.draft.application.response.DraftTemplateSuggestionResponse;
import com.example.draft.application.response.DraftTemplatePresetResponse;
import com.example.draft.domain.BusinessTemplateMapping;
import com.example.draft.domain.DraftTemplatePreset;
import com.example.draft.domain.repository.BusinessTemplateMappingRepository;
import com.example.draft.domain.repository.DraftHistoryRepository;
import com.example.draft.domain.repository.DraftReferenceRepository;
import com.example.draft.domain.repository.DraftTemplatePresetRepository;
import com.example.draft.domain.DraftStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
    private final DraftTemplatePresetRepository presetRepository;
    private final DraftNotificationService notificationService;
    private final DraftAuditPublisher auditPublisher;
    private final com.example.draft.application.business.DraftBusinessPolicy businessPolicy;
    private final ApprovalFacade approvalFacade;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public DraftApplicationService(DraftRepository draftRepository,
                                   ApprovalLineTemplateRepository templateRepository,
                                   DraftFormTemplateRepository formTemplateRepository,
                                   ApprovalGroupRepository approvalGroupRepository,
                                   ApprovalGroupMemberRepository approvalGroupMemberRepository,
                                   BusinessTemplateMappingRepository mappingRepository,
                                   DraftHistoryRepository draftHistoryRepository,
                                   DraftReferenceRepository draftReferenceRepository,
                                   DraftTemplatePresetRepository presetRepository,
                                   DraftNotificationService notificationService,
                                   DraftAuditPublisher auditPublisher,
                                   com.example.draft.application.business.DraftBusinessPolicy businessPolicy,
                                   ApprovalFacade approvalFacade,
                                   org.springframework.context.ApplicationEventPublisher eventPublisher,
                                   ObjectMapper objectMapper,
                                   Clock clock) {
        this.draftRepository = draftRepository;
        this.templateRepository = templateRepository;
        this.formTemplateRepository = formTemplateRepository;
        this.approvalGroupRepository = approvalGroupRepository;
        this.approvalGroupMemberRepository = approvalGroupMemberRepository;
        this.mappingRepository = mappingRepository;
        this.draftHistoryRepository = draftHistoryRepository;
        this.draftReferenceRepository = draftReferenceRepository;
        this.presetRepository = presetRepository;
        this.notificationService = notificationService;
        this.auditPublisher = auditPublisher;
        this.businessPolicy = businessPolicy;
        this.approvalFacade = approvalFacade;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public DraftResponse createDraft(DraftCreateRequest request, String actor, String organizationCode) {
        OffsetDateTime now = now();
        businessPolicy.assertCreatable(request.businessFeatureCode(), organizationCode, actor);
        DraftTemplatePreset preset = resolvePreset(request, organizationCode);
        TemplateSelection selection = selectTemplates(request, organizationCode, preset);
        ApprovalLineTemplate template = selection.approvalTemplate();
        DraftFormTemplate formTemplate = selection.formTemplate();
        Set<String> allowedVariables = allowedVariables(preset);
        String title = resolveTemplateValue(request.title(), preset == null ? null : preset.getTitleTemplate(),
                request.templateVariables(), allowedVariables, actor, organizationCode, now);
        String content = resolveTemplateValue(request.content(), preset == null ? null : preset.getContentTemplate(),
                request.templateVariables(), allowedVariables, actor, organizationCode, now);
        String formPayload = mergeFormPayload(request.formPayload(), preset);
        requireValue(title, "제목은 비어 있을 수 없습니다.");
        requireValue(content, "내용은 비어 있을 수 없습니다.");
        requireValue(formPayload, "폼 데이터는 비어 있을 수 없습니다.");
        Draft draft = Draft.create(title,
                content,
                request.businessFeatureCode(),
                organizationCode,
                template.getTemplateCode(),
                actor,
                now);
        if (preset != null) {
            draft.useTemplatePreset(preset.getId());
        }
        draft.attachFormTemplate(formTemplate, formPayload);
        template.getSteps().stream()
                .sorted(java.util.Comparator.comparingInt(com.example.approval.domain.ApprovalTemplateStep::getStepOrder))
                .map(com.example.draft.domain.DraftApprovalStep::fromTemplate)
                .forEach(draft::addApprovalStep);
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
        requestApprovalIfNeeded(draft, organizationCode, actor);
        publishSubmittedEvent(draft, organizationCode, actor);
        publish(DraftAction.SUBMITTED, draft, actor, null, null, null);
        audit(DraftAction.SUBMITTED, draft, actor, null, organizationCode, null, null);
        businessPolicy.afterStateChanged(draft.getId(), draft.getBusinessFeatureCode(), draft.getStatus(), DraftAction.SUBMITTED, actor);
        return toResponse(draft);
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
        return toResponse(draft);
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
        return toResponse(draft);
    }

    @Transactional
    public DraftResponse cancel(UUID draftId, String actor, String organizationCode) {
        Draft draft = loadDraft(draftId);
        draft.assertOrganizationAccess(organizationCode, false);
        draft.cancel(actor, now());
        publish(DraftAction.CANCELLED, draft, actor, null, null, null);
        audit(DraftAction.CANCELLED, draft, actor, null, organizationCode, null, null);
        businessPolicy.afterStateChanged(draft.getId(), draft.getBusinessFeatureCode(), draft.getStatus(), DraftAction.CANCELLED, actor);
        return toResponse(draft);
    }

    @Transactional
    public DraftResponse withdraw(UUID draftId, String actor, String organizationCode) {
        Draft draft = loadDraft(draftId);
        draft.assertOrganizationAccess(organizationCode, false);
        draft.withdraw(actor, now());
        publish(DraftAction.WITHDRAWN, draft, actor, null, null, null);
        audit(DraftAction.WITHDRAWN, draft, actor, null, organizationCode, null, null);
        businessPolicy.afterStateChanged(draft.getId(), draft.getBusinessFeatureCode(), draft.getStatus(), DraftAction.WITHDRAWN, actor);
        return toResponse(draft);
    }

    @Transactional
    public DraftResponse resubmit(UUID draftId, String actor, String organizationCode) {
        Draft draft = loadDraft(draftId);
        draft.assertOrganizationAccess(organizationCode, false);
        draft.resubmit(actor, now());
        publish(DraftAction.RESUBMITTED, draft, actor, null, null, null);
        audit(DraftAction.RESUBMITTED, draft, actor, null, organizationCode, null, null);
        businessPolicy.afterStateChanged(draft.getId(), draft.getBusinessFeatureCode(), draft.getStatus(), DraftAction.RESUBMITTED, actor);
        return toResponse(draft);
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
        return toResponse(draft);
    }

    @Transactional(readOnly = true)
    public DraftResponse getDraft(UUID draftId, String organizationCode, String requester, boolean auditAccess) {
        Draft draft = loadDraft(draftId);
        draft.assertOrganizationAccess(organizationCode, auditAccess);
        enforceReadAccess(draft, requester, auditAccess);
        return toResponse(draft);
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
        // DataPolicyMatch가 있는 경우 정책 rowScope 우선 적용
        var policyMatch = com.example.common.policy.DataPolicyContextHolder.get();
        if (policyMatch != null && policyMatch.getRowScope() != null) {
            rowScope = RowScope.of(policyMatch.getRowScope(), rowScope);
        }
        rowScope = normalizeRowScope(rowScope);
        Specification<Draft> specification = RowScopeSpecifications.<Draft>organizationScoped(
                "organizationCode",
                rowScope,
                organizationCode,
                scopedOrganizations
        ).and(filter(status, businessFeatureCode, createdBy, titleContains));
        java.util.function.UnaryOperator<String> masker = buildMasker(policyMatch);
        return draftRepository.findAll(specification, pageable)
                .map(draft -> toResponse(draft, masker));
    }

    private java.util.function.UnaryOperator<String> buildMasker(com.example.common.policy.DataPolicyMatch match) {
        if (match == null || match.getMaskRule() == null) {
            return java.util.function.UnaryOperator.identity();
        }
        String rule = match.getMaskRule();
        String params = match.getMaskParams();
        return value -> com.example.common.masking.MaskRuleProcessor.apply(rule, value, params);
    }

    @Transactional(readOnly = true)
    public DraftTemplateSuggestionResponse suggestTemplate(String businessFeatureCode, String organizationCode) {
        BusinessTemplateMapping mapping = mappingRepository.findByBusinessFeatureCodeAndOrganizationCodeAndActiveTrue(businessFeatureCode, organizationCode)
                .or(() -> mappingRepository.findByBusinessFeatureCodeAndOrganizationCodeIsNullAndActiveTrue(businessFeatureCode))
                .orElseThrow(() -> new DraftTemplateNotFoundException("기본 매핑된 템플릿이 없습니다."));
        return DraftTemplateSuggestionResponse.from(mapping);
    }

    @Transactional(readOnly = true)
    public List<DraftTemplatePresetResponse> listTemplatePresets(String businessFeatureCode,
                                                                 String organizationCode,
                                                                 boolean auditAccess) {
        List<DraftTemplatePreset> presets = new java.util.ArrayList<>();
        presets.addAll(presetRepository.findByBusinessFeatureCodeAndOrganizationCodeIsNullAndActiveTrue(businessFeatureCode));
        presets.addAll(presetRepository.findByBusinessFeatureCodeAndOrganizationCodeAndActiveTrue(businessFeatureCode, organizationCode));
        return presets.stream()
                .filter(p -> auditAccess || p.isGlobal() || organizationCode.equals(p.getOrganizationCode()))
                .map(this::toPresetResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DraftTemplatePresetResponse> recommendTemplatePresets(String businessFeatureCode,
                                                                      String organizationCode,
                                                                      String actor) {
        List<DraftTemplatePreset> orgPresets = presetRepository.findByBusinessFeatureCodeAndOrganizationCodeAndActiveTrue(businessFeatureCode, organizationCode);
        List<DraftTemplatePreset> globalPresets = presetRepository.findByBusinessFeatureCodeAndOrganizationCodeIsNullAndActiveTrue(businessFeatureCode);
        java.util.Map<UUID, DraftTemplatePreset> presetMap = java.util.stream.Stream.concat(orgPresets.stream(), globalPresets.stream())
                .collect(java.util.stream.Collectors.toMap(DraftTemplatePreset::getId, java.util.function.Function.identity()));
        java.util.List<DraftTemplatePreset> ordered = new java.util.ArrayList<>();
        java.util.Set<UUID> seen = new java.util.HashSet<>();

        draftRepository.findTop5ByCreatedByAndBusinessFeatureCodeOrderByCreatedAtDesc(actor, businessFeatureCode).stream()
                .map(Draft::getTemplatePresetId)
                .filter(java.util.Objects::nonNull)
                .map(presetMap::get)
                .filter(java.util.Objects::nonNull)
                .forEach(preset -> {
                    if (seen.add(preset.getId())) {
                        ordered.add(preset);
                    }
                });

        orgPresets.stream()
                .filter(p -> seen.add(p.getId()))
                .forEach(ordered::add);
        globalPresets.stream()
                .filter(p -> seen.add(p.getId()))
                .forEach(ordered::add);

        return ordered.stream()
                .map(this::toPresetResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DraftHistoryResponse> listHistory(UUID draftId, String organizationCode, String requester, boolean auditAccess) {
        Draft draft = loadDraft(draftId);
        draft.assertOrganizationAccess(organizationCode, auditAccess);
        enforceReadAccess(draft, requester, auditAccess);
        java.util.function.UnaryOperator<String> masker = buildMasker(com.example.common.policy.DataPolicyContextHolder.get());
        return draftHistoryRepository.findByDraftIdOrderByOccurredAtAsc(draftId)
                .stream()
                .map(h -> DraftHistoryResponse.from(h, masker))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DraftReferenceResponse> listReferences(UUID draftId, String organizationCode, String requester, boolean auditAccess) {
        Draft draft = loadDraft(draftId);
        draft.assertOrganizationAccess(organizationCode, auditAccess);
        enforceReadAccess(draft, requester, auditAccess);
        java.util.function.UnaryOperator<String> masker = buildMasker(com.example.common.policy.DataPolicyContextHolder.get());
        return draftReferenceRepository.findByDraftIdAndActiveTrue(draftId)
                .stream()
                .map(ref -> DraftReferenceResponse.from(ref, masker))
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

    private DraftResponse toResponse(Draft draft) {
        return toResponse(draft, UnaryOperator.identity());
    }

    private DraftResponse toResponse(Draft draft, UnaryOperator<String> masker) {
        ApprovalStatusSnapshot status = approvalFacade.findByDraftId(draft.getId());
        return DraftResponse.from(draft, status, masker);
    }

    private void requestApprovalIfNeeded(Draft draft, String organizationCode, String actor) {
        if (draft.getApprovalRequestId() != null) {
            return;
        }
        var groupCodes = draft.getApprovalSteps().stream()
                .map(DraftApprovalStep::getApprovalGroupCode)
                .toList();
        ApprovalStatusSnapshot snapshot = approvalFacade.requestApproval(new ApprovalRequestCommand(
                draft.getId(),
                draft.getTemplateCode(),
                organizationCode,
                actor,
                draft.getTitle(),
                groupCodes
        ));
        draft.linkApprovalRequest(snapshot.approvalRequestId());
    }

    private void publishSubmittedEvent(Draft draft, String organizationCode, String actor) {
        DraftSubmittedEvent event = new DraftSubmittedEvent(
                draft.getId(),
                draft.getTemplateCode(),
                organizationCode,
                actor,
                draft.getTitle(),
                draft.getApprovalSteps().stream().map(DraftApprovalStep::getApprovalGroupCode).toList()
        );
        eventPublisher.publishEvent(event);
        // Kafka 퍼블리셔는 EventListener(DraftSubmittedEventBridge)에서 전송
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

    private TemplateSelection selectTemplates(DraftCreateRequest request, String organizationCode, DraftTemplatePreset preset) {
        if (request.templateId() != null && request.formTemplateId() != null) {
            ApprovalLineTemplate template = templateRepository.findByIdAndActiveTrue(request.templateId())
                    .orElseThrow(() -> new DraftTemplateNotFoundException("결재선 템플릿을 찾을 수 없습니다."));
            template.assertOrganization(organizationCode);
            DraftFormTemplate formTemplate = formTemplateRepository.findByIdAndActiveTrue(request.formTemplateId())
                    .orElseThrow(() -> new DraftTemplateNotFoundException("기안 양식을 찾을 수 없습니다."));
            formTemplate.assertOrganization(organizationCode);
            ensureBusinessMatches(formTemplate, request.businessFeatureCode());
            return new TemplateSelection(template, formTemplate);
        }

        ApprovalLineTemplate approvalTemplate = null;
        DraftFormTemplate formTemplate = null;

        if (preset != null) {
            approvalTemplate = preset.getDefaultApprovalTemplate();
            formTemplate = preset.getFormTemplate();
            if (approvalTemplate != null) {
                approvalTemplate.assertOrganization(organizationCode);
            }
            formTemplate.assertOrganization(organizationCode);
            ensureBusinessMatches(formTemplate, request.businessFeatureCode());
        }

        BusinessTemplateMapping mapping = null;
        if (approvalTemplate == null || formTemplate == null) {
            mapping = mappingRepository.findByBusinessFeatureCodeAndOrganizationCodeAndActiveTrue(request.businessFeatureCode(), organizationCode)
                    .or(() -> mappingRepository.findByBusinessFeatureCodeAndOrganizationCodeIsNullAndActiveTrue(request.businessFeatureCode()))
                    .orElseThrow(() -> new DraftTemplateNotFoundException("기본 매핑된 템플릿을 찾을 수 없습니다."));
        }

        if (approvalTemplate == null && mapping != null) {
            mapping.getApprovalLineTemplate().assertOrganization(organizationCode);
            approvalTemplate = mapping.getApprovalLineTemplate();
        }
        if (formTemplate == null && mapping != null) {
            mapping.getDraftFormTemplate().assertOrganization(organizationCode);
            formTemplate = mapping.getDraftFormTemplate();
        }

        if (approvalTemplate == null || formTemplate == null) {
            throw new DraftTemplateNotFoundException("적용 가능한 결재선/양식 템플릿을 찾을 수 없습니다.");
        }
        ensureBusinessMatches(formTemplate, request.businessFeatureCode());
        return new TemplateSelection(approvalTemplate, formTemplate);
    }

    private record TemplateSelection(ApprovalLineTemplate approvalTemplate, DraftFormTemplate formTemplate) {
    }

    private DraftTemplatePreset resolvePreset(DraftCreateRequest request, String organizationCode) {
        if (request.templatePresetId() == null) {
            return null;
        }
        DraftTemplatePreset preset = presetRepository.findByIdAndActiveTrue(request.templatePresetId())
                .orElseThrow(() -> new DraftTemplateNotFoundException("템플릿 프리셋을 찾을 수 없습니다."));
        preset.assertOrganization(organizationCode);
        if (!preset.matchesBusiness(request.businessFeatureCode())) {
            throw new DraftTemplateNotFoundException("비즈니스 유형에 맞는 템플릿 프리셋이 아닙니다.");
        }
        return preset;
    }

    private Set<String> allowedVariables(DraftTemplatePreset preset) {
        if (preset == null || preset.getVariablesJson() == null || preset.getVariablesJson().isBlank()) {
            return Set.of();
        }
        try {
            List<String> variables = objectMapper.readValue(preset.getVariablesJson(), new TypeReference<List<String>>() {
            });
            return new HashSet<>(variables);
        } catch (Exception ex) {
            return Set.of();
        }
    }

    private String resolveTemplateValue(String requestValue,
                                        String templateValue,
                                        Map<String, String> variables,
                                        Set<String> allowedVariables,
                                        String actor,
                                        String organizationCode,
                                        OffsetDateTime now) {
        if (templateValue == null) {
            return requestValue;
        }
        Map<String, String> resolved = buildVariableValues(variables, allowedVariables, actor, organizationCode, now);
        return replacePlaceholders(templateValue, resolved, requestValue);
    }

    private Map<String, String> buildVariableValues(Map<String, String> variables,
                                                    Set<String> allowedVariables,
                                                    String actor,
                                                    String organizationCode,
                                                    OffsetDateTime now) {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        map.put("작성자", actor);
        map.put("오늘", now.toLocalDate().toString());
        map.put("조직명", organizationCode);
        map.put("부서", organizationCode);
        boolean allowCustom = allowedVariables != null && !allowedVariables.isEmpty();
        if (allowCustom && variables != null) {
            variables.forEach((key, value) -> {
                if (allowedVariables.contains(key) && value != null && !value.isBlank()) {
                    map.put(key, value);
                }
            });
        }
        return map;
    }

    private DraftTemplatePresetResponse toPresetResponse(DraftTemplatePreset preset) {
        return DraftTemplatePresetResponse.from(preset, variablesFromPreset(preset), UnaryOperator.identity());
    }

    private List<String> variablesFromPreset(DraftTemplatePreset preset) {
        if (preset == null || preset.getVariablesJson() == null || preset.getVariablesJson().isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(preset.getVariablesJson(), new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private String replacePlaceholders(String template, Map<String, String> values, String fallback) {
        Pattern pattern = Pattern.compile("\\{([^}]+)}");
        Matcher matcher = pattern.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = values.getOrDefault(key, matcher.group(0));
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        String result = sb.toString();
        if (result == null || result.isBlank()) {
            return fallback;
        }
        return result;
    }

    private String mergeFormPayload(String userPayload, DraftTemplatePreset preset) {
        if (preset == null) {
            return userPayload;
        }
        JsonNode base = readJsonOrEmpty(preset.getDefaultFormPayload());
        JsonNode user = readJsonOrEmpty(userPayload);
        if (!(base instanceof ObjectNode baseObj) || !(user instanceof ObjectNode userObj)) {
            return userPayload == null || userPayload.isBlank() ? preset.getDefaultFormPayload() : userPayload;
        }
        ObjectNode merged = baseObj.deepCopy();
        deepMerge(merged, userObj);
        try {
            return objectMapper.writeValueAsString(merged);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("폼 데이터를 직렬화할 수 없습니다.", e);
        }
    }

    private JsonNode readJsonOrEmpty(String payload) {
        if (payload == null || payload.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("formPayload는 JSON 형식이어야 합니다.", e);
        }
    }

    private void deepMerge(ObjectNode target, JsonNode update) {
        update.fields().forEachRemaining(entry -> {
            String field = entry.getKey();
            JsonNode value = entry.getValue();
            if (value.isObject() && target.get(field) != null && target.get(field).isObject()) {
                deepMerge((ObjectNode) target.get(field), value);
            } else {
                target.set(field, value);
            }
        });
    }

    private void ensureBusinessMatches(DraftFormTemplate formTemplate, String businessFeatureCode) {
        if (!formTemplate.matchesBusiness(businessFeatureCode)) {
            throw new DraftTemplateNotFoundException("비즈니스 유형에 맞는 기안 양식이 아닙니다.");
        }
    }

    private void requireValue(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
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
