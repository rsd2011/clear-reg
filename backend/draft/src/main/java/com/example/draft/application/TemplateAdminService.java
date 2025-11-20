package com.example.draft.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.auth.permission.context.AuthContext;
import com.example.common.security.RowScope;
import com.example.draft.application.request.ApprovalGroupRequest;
import com.example.draft.application.request.ApprovalLineTemplateRequest;
import com.example.draft.application.request.ApprovalTemplateStepRequest;
import com.example.draft.application.request.DraftFormTemplateRequest;
import com.example.draft.application.response.ApprovalGroupResponse;
import com.example.draft.application.response.ApprovalLineTemplateResponse;
import com.example.draft.application.response.DraftFormTemplateResponse;
import com.example.draft.domain.ApprovalGroup;
import com.example.draft.domain.ApprovalLineTemplate;
import com.example.draft.domain.ApprovalTemplateStep;
import com.example.draft.domain.DraftFormTemplate;
import com.example.draft.domain.exception.DraftAccessDeniedException;
import com.example.draft.domain.exception.DraftTemplateNotFoundException;
import com.example.draft.domain.repository.ApprovalGroupRepository;
import com.example.draft.domain.repository.ApprovalLineTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRepository;

@Service
@Transactional
public class TemplateAdminService {

    private final ApprovalGroupRepository approvalGroupRepository;
    private final ApprovalLineTemplateRepository approvalLineTemplateRepository;
    private final DraftFormTemplateRepository draftFormTemplateRepository;

    public TemplateAdminService(ApprovalGroupRepository approvalGroupRepository,
                                ApprovalLineTemplateRepository approvalLineTemplateRepository,
                                DraftFormTemplateRepository draftFormTemplateRepository) {
        this.approvalGroupRepository = approvalGroupRepository;
        this.approvalLineTemplateRepository = approvalLineTemplateRepository;
        this.draftFormTemplateRepository = draftFormTemplateRepository;
    }

    public ApprovalGroupResponse createApprovalGroup(ApprovalGroupRequest request, AuthContext context, boolean audit) {
        String org = ensureOrganization(request.organizationCode(), context, audit);
        OffsetDateTime now = OffsetDateTime.now();
        ApprovalGroup group = ApprovalGroup.create(
                request.groupCode(),
                request.name(),
                request.description(),
                org,
                request.conditionExpression(),
                now);
        return ApprovalGroupResponse.from(approvalGroupRepository.save(group));
    }

    public ApprovalGroupResponse updateApprovalGroup(UUID id, ApprovalGroupRequest request, AuthContext context, boolean audit) {
        ApprovalGroup group = approvalGroupRepository.findById(id)
                .orElseThrow(() -> new DraftTemplateNotFoundException("결재 그룹을 찾을 수 없습니다."));
        ensureOrganizationAccess(group.getOrganizationCode(), context, audit);
        group.rename(request.name(), request.description(), request.conditionExpression(), OffsetDateTime.now());
        return ApprovalGroupResponse.from(group);
    }

    @Transactional(readOnly = true)
    public List<ApprovalGroupResponse> listApprovalGroups(String organizationCode, AuthContext context, boolean audit) {
        if (!audit) {
            organizationCode = context.organizationCode();
        }
        String orgFilter = organizationCode;
        return approvalGroupRepository.findAll().stream()
                .filter(g -> orgFilter == null || orgFilter.equals(g.getOrganizationCode()))
                .map(ApprovalGroupResponse::from)
                .toList();
    }

    public ApprovalLineTemplateResponse createApprovalLineTemplate(ApprovalLineTemplateRequest request,
                                                                    AuthContext context,
                                                                    boolean audit) {
        String org = ensureOrganizationNullable(request.organizationCode(), context, audit);
        OffsetDateTime now = OffsetDateTime.now();
        ApprovalLineTemplate template = org == null
                ? ApprovalLineTemplate.createGlobal(request.name(), request.businessType(), now)
                : ApprovalLineTemplate.create(request.name(), request.businessType(), org, now);
        template.rename(request.name(), request.active(), now);
        template.replaceSteps(toEntities(template, request.steps()));
        return ApprovalLineTemplateResponse.from(approvalLineTemplateRepository.save(template));
    }

    public ApprovalLineTemplateResponse updateApprovalLineTemplate(UUID id,
                                                                    ApprovalLineTemplateRequest request,
                                                                    AuthContext context,
                                                                    boolean audit) {
        ApprovalLineTemplate template = approvalLineTemplateRepository.findById(id)
                .orElseThrow(() -> new DraftTemplateNotFoundException("결재선 템플릿을 찾을 수 없습니다."));
        if (template.getOrganizationCode() != null) {
            ensureOrganizationAccess(template.getOrganizationCode(), context, audit);
        }
        OffsetDateTime now = OffsetDateTime.now();
        template.rename(request.name(), request.active(), now);
        template.replaceSteps(toEntities(template, request.steps()));
        return ApprovalLineTemplateResponse.from(template);
    }

    @Transactional(readOnly = true)
    public List<ApprovalLineTemplateResponse> listApprovalLineTemplates(String businessType,
                                                                         String organizationCode,
                                                                         boolean activeOnly,
                                                                         AuthContext context,
                                                                         boolean audit) {
        if (!audit) {
            organizationCode = context.organizationCode();
        }
        String orgFilter = organizationCode;
        return approvalLineTemplateRepository.findAll().stream()
                .filter(t -> businessType == null || businessType.equalsIgnoreCase(t.getBusinessType()))
                .filter(t -> orgFilter == null || t.applicableTo(orgFilter))
                .filter(t -> !activeOnly || t.isActive())
                .map(ApprovalLineTemplateResponse::from)
                .toList();
    }

    public DraftFormTemplateResponse createDraftFormTemplate(DraftFormTemplateRequest request,
                                                              AuthContext context,
                                                              boolean audit) {
        String org = ensureOrganizationNullable(request.organizationCode(), context, audit);
        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplate template = DraftFormTemplate.create(
                request.name(),
                request.businessType(),
                org,
                request.schemaJson(),
                now);
        if (!request.active()) {
            template.update(template.getName(), template.getSchemaJson(), request.active(), now);
        }
        return DraftFormTemplateResponse.from(draftFormTemplateRepository.save(template));
    }

    public DraftFormTemplateResponse updateDraftFormTemplate(UUID id,
                                                              DraftFormTemplateRequest request,
                                                              AuthContext context,
                                                              boolean audit) {
        DraftFormTemplate template = draftFormTemplateRepository.findById(id)
                .orElseThrow(() -> new DraftTemplateNotFoundException("기안 양식 템플릿을 찾을 수 없습니다."));
        if (template.getOrganizationCode() != null) {
            ensureOrganizationAccess(template.getOrganizationCode(), context, audit);
        }
        template.update(request.name(), request.schemaJson(), request.active(), OffsetDateTime.now());
        return DraftFormTemplateResponse.from(template);
    }

    @Transactional(readOnly = true)
    public List<DraftFormTemplateResponse> listDraftFormTemplates(String businessType,
                                                                  String organizationCode,
                                                                  boolean activeOnly,
                                                                  AuthContext context,
                                                                  boolean audit) {
        if (!audit) {
            organizationCode = context.organizationCode();
        }
        String orgFilter = organizationCode;
        return draftFormTemplateRepository.findAll().stream()
                .filter(t -> businessType == null || businessType.equalsIgnoreCase(t.getBusinessType()))
                .filter(t -> orgFilter == null || t.getOrganizationCode() == null || orgFilter.equals(t.getOrganizationCode()))
                .filter(t -> !activeOnly || t.isActive())
                .map(DraftFormTemplateResponse::from)
                .toList();
    }

    private List<ApprovalTemplateStep> toEntities(ApprovalLineTemplate template, List<ApprovalTemplateStepRequest> steps) {
        return steps.stream()
                .map(req -> new ApprovalTemplateStep(template, req.stepOrder(), req.approvalGroupCode(), req.description()))
                .toList();
    }

    private String ensureOrganization(String organizationCode, AuthContext context, boolean audit) {
        if (organizationCode == null || organizationCode.isBlank()) {
            throw new IllegalArgumentException("organizationCode는 필수입니다.");
        }
        ensureOrganizationAccess(organizationCode, context, audit);
        return organizationCode;
    }

    private String ensureOrganizationNullable(String organizationCode, AuthContext context, boolean audit) {
        if (organizationCode == null || organizationCode.isBlank()) {
            return null;
        }
        ensureOrganizationAccess(organizationCode, context, audit);
        return organizationCode;
    }

    private void ensureOrganizationAccess(String organizationCode, AuthContext context, boolean audit) {
        if (audit) {
            return;
        }
        if (context.rowScope() != RowScope.ALL && !organizationCode.equals(context.organizationCode())) {
            throw new DraftAccessDeniedException("해당 조직에 대한 권한이 없습니다.");
        }
    }
}
