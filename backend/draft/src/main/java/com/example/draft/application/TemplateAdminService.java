package com.example.draft.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.admin.permission.context.AuthContext;
import com.example.common.orggroup.WorkType;
import com.example.common.version.ChangeAction;
import com.example.admin.approval.dto.ApprovalTemplateRootRequest;
import com.example.admin.approval.dto.ApprovalTemplateRootResponse;
import com.example.admin.approval.service.ApprovalTemplateRootService;
import com.example.draft.application.dto.DraftFormTemplateRequest;
import com.example.draft.application.dto.DraftFormTemplateResponse;
import com.example.admin.draft.domain.DraftFormTemplate;
import com.example.admin.draft.domain.DraftFormTemplateRoot;
import com.example.draft.domain.exception.DraftTemplateNotFoundException;
import com.example.draft.domain.repository.DraftFormTemplateRepository;
import com.example.draft.domain.repository.DraftFormTemplateRootRepository;

@Service
@Transactional
public class TemplateAdminService {

    private final ApprovalTemplateRootService approvalTemplateRootService;
    private final DraftFormTemplateRepository draftFormTemplateRepository;
    private final DraftFormTemplateRootRepository draftFormTemplateRootRepository;

    public TemplateAdminService(ApprovalTemplateRootService approvalTemplateRootService,
                                DraftFormTemplateRepository draftFormTemplateRepository,
                                DraftFormTemplateRootRepository draftFormTemplateRootRepository) {
        this.approvalTemplateRootService = approvalTemplateRootService;
        this.draftFormTemplateRepository = draftFormTemplateRepository;
        this.draftFormTemplateRootRepository = draftFormTemplateRootRepository;
    }

    public ApprovalTemplateRootResponse createApprovalTemplateRoot(ApprovalTemplateRootRequest request,
                                                                    AuthContext context,
                                                                    boolean audit) {
        return approvalTemplateRootService.create(request, context);
    }

    public ApprovalTemplateRootResponse updateApprovalTemplateRoot(UUID id,
                                                                    ApprovalTemplateRootRequest request,
                                                                    AuthContext context,
                                                                    boolean audit) {
        return approvalTemplateRootService.update(id, request, context);
    }

    @Transactional(readOnly = true)
    public List<ApprovalTemplateRootResponse> listApprovalTemplateRoots(String businessType,
                                                                         String organizationCode,
                                                                         boolean activeOnly,
                                                                         AuthContext context,
                                                                         boolean audit) {
        return approvalTemplateRootService.list(null, activeOnly);
    }

    /**
     * 기안 양식 템플릿을 생성한다.
     * Root와 첫 번째 버전을 함께 생성합니다.
     */
    public DraftFormTemplateResponse createDraftFormTemplate(DraftFormTemplateRequest request,
                                                              AuthContext context,
                                                              boolean audit) {
        OffsetDateTime now = OffsetDateTime.now();
        
        // Root 생성
        DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);
        draftFormTemplateRootRepository.save(root);
        
        // 첫 번째 버전 생성
        DraftFormTemplate template = DraftFormTemplate.create(
                root,
                1,
                request.name(),
                request.workType(),
                request.schemaJson(),
                request.active(),
                ChangeAction.CREATE,
                request.changeReason(),
                context.username(),
                context.username(),
                now);
        draftFormTemplateRepository.save(template);

        // Root에 현재 버전 설정
        root.activateNewVersion(template, now);
        
        return DraftFormTemplateResponse.from(template);
    }

    /**
     * 기안 양식 템플릿을 수정한다.
     * 새 버전을 생성하고 이전 버전을 닫습니다.
     */
    public DraftFormTemplateResponse updateDraftFormTemplate(UUID rootId,
                                                              DraftFormTemplateRequest request,
                                                              AuthContext context,
                                                              boolean audit) {
        DraftFormTemplateRoot root = draftFormTemplateRootRepository.findById(rootId)
                .orElseThrow(() -> new DraftTemplateNotFoundException("기안 양식 템플릿을 찾을 수 없습니다."));
        
        OffsetDateTime now = OffsetDateTime.now();
        
        // 현재 최대 버전 조회
        Integer maxVersion = draftFormTemplateRepository.findMaxVersionByRoot(root);
        int newVersion = (maxVersion != null ? maxVersion : 0) + 1;
        
        // 새 버전 생성
        DraftFormTemplate newTemplate = DraftFormTemplate.create(
                root,
                newVersion,
                request.name(),
                request.workType(),
                request.schemaJson(),
                request.active(),
                ChangeAction.UPDATE,
                request.changeReason(),
                context.username(),
                context.username(),
                now);
        draftFormTemplateRepository.save(newTemplate);
        
        // 버전 전환
        root.activateNewVersion(newTemplate, now);
        
        return DraftFormTemplateResponse.from(newTemplate);
    }

    /**
     * 기안 양식 템플릿 목록을 조회한다.
     */
    @Transactional(readOnly = true)
    public List<DraftFormTemplateResponse> listDraftFormTemplates(WorkType workType,
                                                                  boolean activeOnly,
                                                                  AuthContext context,
                                                                  boolean audit) {
        List<DraftFormTemplate> templates;
        if (workType != null) {
            templates = draftFormTemplateRepository.findCurrentByWorkType(workType);
        } else {
            templates = draftFormTemplateRepository.findAllCurrent();
        }
        
        return templates.stream()
                .filter(t -> !activeOnly || t.isActive())
                .map(DraftFormTemplateResponse::from)
                .toList();
    }

    /**
     * 기안 양식 템플릿 루트 목록을 조회한다.
     */
    @Transactional(readOnly = true)
    public List<DraftFormTemplateRoot> listDraftFormTemplateRoots(WorkType workType, boolean activeOnly) {
        if (workType != null) {
            return draftFormTemplateRootRepository.findByWorkTypeAndActive(workType);
        }
        return draftFormTemplateRootRepository.findAllActive();
    }

    /**
     * 템플릿 코드로 Root를 조회한다.
     */
    @Transactional(readOnly = true)
    public DraftFormTemplateRoot findRootByTemplateCode(String templateCode) {
        return draftFormTemplateRootRepository.findByTemplateCode(templateCode)
                .orElseThrow(() -> new DraftTemplateNotFoundException("기안 양식 템플릿을 찾을 수 없습니다: " + templateCode));
    }

    /**
     * 초안 버전을 생성한다.
     */
    public DraftFormTemplateResponse createDraft(UUID rootId,
                                                  DraftFormTemplateRequest request,
                                                  AuthContext context) {
        DraftFormTemplateRoot root = draftFormTemplateRootRepository.findById(rootId)
                .orElseThrow(() -> new DraftTemplateNotFoundException("기안 양식 템플릿을 찾을 수 없습니다."));
        
        if (root.hasDraft()) {
            throw new IllegalStateException("이미 초안 버전이 존재합니다.");
        }
        
        OffsetDateTime now = OffsetDateTime.now();
        Integer maxVersion = draftFormTemplateRepository.findMaxVersionByRoot(root);
        int newVersion = (maxVersion != null ? maxVersion : 0) + 1;
        
        DraftFormTemplate draft = DraftFormTemplate.createDraft(
                root,
                newVersion,
                request.name(),
                request.workType(),
                request.schemaJson(),
                request.active(),
                request.changeReason(),
                context.username(),
                context.username(),
                now);
        draftFormTemplateRepository.save(draft);
        
        root.setDraftVersion(draft);
        
        return DraftFormTemplateResponse.from(draft);
    }

    /**
     * 초안을 게시한다.
     */
    public DraftFormTemplateResponse publishDraft(UUID rootId, AuthContext context) {
        DraftFormTemplateRoot root = draftFormTemplateRootRepository.findById(rootId)
                .orElseThrow(() -> new DraftTemplateNotFoundException("기안 양식 템플릿을 찾을 수 없습니다."));
        
        OffsetDateTime now = OffsetDateTime.now();
        root.publishDraft(now);
        
        return DraftFormTemplateResponse.from(root.getCurrentVersion());
    }

    /**
     * 초안을 삭제한다.
     */
    public void discardDraft(UUID rootId, AuthContext context) {
        DraftFormTemplateRoot root = draftFormTemplateRootRepository.findById(rootId)
                .orElseThrow(() -> new DraftTemplateNotFoundException("기안 양식 템플릿을 찾을 수 없습니다."));
        
        root.discardDraft();
    }
}
