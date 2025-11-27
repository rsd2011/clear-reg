package com.example.draft.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.admin.permission.context.AuthContext;
import com.example.common.security.RowScope;
import com.example.admin.approval.ApprovalLineTemplate;
import com.example.admin.approval.ApprovalLineTemplateRepository;
import com.example.admin.approval.ApprovalTemplateStep;
import com.example.admin.approval.dto.ApprovalLineTemplateRequest;
import com.example.admin.approval.dto.ApprovalLineTemplateResponse;
import com.example.admin.approval.dto.ApprovalTemplateStepRequest;
import com.example.draft.application.dto.DraftFormTemplateRequest;
import com.example.draft.application.dto.DraftTemplatePresetRequest;
import com.example.draft.application.dto.DraftFormTemplateResponse;
import com.example.draft.application.dto.DraftTemplatePresetResponse;
import com.example.draft.domain.DraftFormTemplate;
import com.example.draft.domain.DraftTemplatePreset;
import com.example.draft.domain.exception.DraftAccessDeniedException;
import com.example.draft.domain.exception.DraftTemplateNotFoundException;
import com.example.draft.domain.repository.DraftFormTemplateRepository;
import com.example.draft.domain.repository.DraftTemplatePresetRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional
public class TemplateAdminService {

    private final ApprovalLineTemplateRepository approvalLineTemplateRepository;
    private final DraftFormTemplateRepository draftFormTemplateRepository;
    private final DraftTemplatePresetRepository presetRepository;
    private final ObjectMapper objectMapper;

    public TemplateAdminService(ApprovalLineTemplateRepository approvalLineTemplateRepository,
                                DraftFormTemplateRepository draftFormTemplateRepository,
                                DraftTemplatePresetRepository presetRepository,
                                ObjectMapper objectMapper) {
        this.approvalLineTemplateRepository = approvalLineTemplateRepository;
        this.draftFormTemplateRepository = draftFormTemplateRepository;
        this.presetRepository = presetRepository;
        this.objectMapper = objectMapper;
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

    public DraftTemplatePresetResponse createDraftTemplatePreset(DraftTemplatePresetRequest request,
                                                                  AuthContext context,
                                                                  boolean audit) {
        String org = ensureOrganizationNullable(request.organizationCode(), context, audit);
        OffsetDateTime now = OffsetDateTime.now();
        DraftFormTemplate formTemplate = draftFormTemplateRepository.findByIdAndActiveTrue(request.formTemplateId())
                .orElseThrow(() -> new DraftTemplateNotFoundException("기안 양식을 찾을 수 없습니다."));
        formTemplate.assertOrganization(org == null ? context.organizationCode() : org);
        ensureBusinessMatches(formTemplate, request.businessFeatureCode());
        ApprovalLineTemplate approvalTemplate = null;
        if (request.defaultApprovalTemplateId() != null) {
            approvalTemplate = approvalLineTemplateRepository.findByIdAndActiveTrue(request.defaultApprovalTemplateId())
                    .orElseThrow(() -> new DraftTemplateNotFoundException("결재선 템플릿을 찾을 수 없습니다."));
            if (approvalTemplate.getOrganizationCode() != null) {
                ensureOrganizationAccess(approvalTemplate.getOrganizationCode(), context, audit);
            }
            ensureApprovalBusiness(approvalTemplate, request.businessFeatureCode());
        }
        DraftTemplatePreset preset = DraftTemplatePreset.create(
                request.name(),
                request.businessFeatureCode(),
                org,
                request.titleTemplate(),
                request.contentTemplate(),
                formTemplate,
                approvalTemplate,
                request.defaultFormPayload(),
                serializeVariables(request.variables()),
                request.active(),
                now);
        return DraftTemplatePresetResponse.from(presetRepository.save(preset), request.variables(), java.util.function.UnaryOperator.identity());
    }

    public DraftTemplatePresetResponse updateDraftTemplatePreset(UUID id,
                                                                  DraftTemplatePresetRequest request,
                                                                  AuthContext context,
                                                                  boolean audit) {
        DraftTemplatePreset preset = presetRepository.findById(id)
                .orElseThrow(() -> new DraftTemplateNotFoundException("템플릿 프리셋을 찾을 수 없습니다."));
        if (preset.getOrganizationCode() != null) {
            ensureOrganizationAccess(preset.getOrganizationCode(), context, audit);
        }
        if (!preset.getBusinessFeatureCode().equalsIgnoreCase(request.businessFeatureCode())) {
            throw new IllegalArgumentException("businessFeatureCode는 변경할 수 없습니다.");
        }
        DraftFormTemplate formTemplate = draftFormTemplateRepository.findByIdAndActiveTrue(request.formTemplateId())
                .orElseThrow(() -> new DraftTemplateNotFoundException("기안 양식을 찾을 수 없습니다."));
        formTemplate.assertOrganization(preset.getOrganizationCode() == null ? context.organizationCode() : preset.getOrganizationCode());
        ensureBusinessMatches(formTemplate, preset.getBusinessFeatureCode());
        ApprovalLineTemplate approvalTemplate = null;
        if (request.defaultApprovalTemplateId() != null) {
            approvalTemplate = approvalLineTemplateRepository.findByIdAndActiveTrue(request.defaultApprovalTemplateId())
                    .orElseThrow(() -> new DraftTemplateNotFoundException("결재선 템플릿을 찾을 수 없습니다."));
            if (approvalTemplate.getOrganizationCode() != null) {
                ensureOrganizationAccess(approvalTemplate.getOrganizationCode(), context, audit);
            }
            ensureApprovalBusiness(approvalTemplate, preset.getBusinessFeatureCode());
        }
        preset.update(request.name(),
                request.titleTemplate(),
                request.contentTemplate(),
                formTemplate,
                approvalTemplate,
                request.defaultFormPayload(),
                serializeVariables(request.variables()),
                request.active(),
                OffsetDateTime.now());
        return DraftTemplatePresetResponse.from(preset, request.variables(), java.util.function.UnaryOperator.identity());
    }

    @Transactional(readOnly = true)
    public List<DraftTemplatePresetResponse> listDraftTemplatePresets(String businessType,
                                                                      String organizationCode,
                                                                      boolean activeOnly,
                                                                      AuthContext context,
                                                                      boolean audit) {
        if (!audit) {
            organizationCode = context.organizationCode();
        }
        String orgFilter = organizationCode;
        return presetRepository.findAll().stream()
                .filter(p -> businessType == null || businessType.equalsIgnoreCase(p.getBusinessFeatureCode()))
                .filter(p -> orgFilter == null || p.isGlobal() || orgFilter.equals(p.getOrganizationCode()))
                .filter(p -> !activeOnly || p.isActive())
                .map(this::toResponse)
                .toList();
    }

    private List<ApprovalTemplateStep> toEntities(ApprovalLineTemplate template, List<ApprovalTemplateStepRequest> steps) {
        return steps.stream()
                .map(req -> new ApprovalTemplateStep(template, req.stepOrder(), req.approvalGroupCode(), req.description()))
                .toList();
    }

    private DraftTemplatePresetResponse toResponse(DraftTemplatePreset preset) {
        return DraftTemplatePresetResponse.from(preset, deserializeVariables(preset.getVariablesJson()), java.util.function.UnaryOperator.identity());
    }

    private List<String> deserializeVariables(String variablesJson) {
        if (variablesJson == null || variablesJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(variablesJson, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private String serializeVariables(List<String> variables) {
        if (variables == null || variables.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(variables);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("variables 직렬화에 실패했습니다.", e);
        }
    }

    private void ensureBusinessMatches(DraftFormTemplate formTemplate, String businessFeatureCode) {
        if (!formTemplate.matchesBusiness(businessFeatureCode)) {
            throw new IllegalArgumentException("비즈니스 유형에 맞지 않는 양식 템플릿입니다.");
        }
    }

    private void ensureApprovalBusiness(ApprovalLineTemplate template, String businessFeatureCode) {
        if (!template.getBusinessType().equalsIgnoreCase(businessFeatureCode)) {
            throw new IllegalArgumentException("비즈니스 유형에 맞지 않는 결재선 템플릿입니다.");
        }
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
