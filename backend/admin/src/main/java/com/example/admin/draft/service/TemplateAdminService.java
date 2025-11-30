package com.example.admin.draft.service;

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
import com.example.admin.draft.dto.DraftFormTemplateRequest;
import com.example.admin.draft.dto.DraftFormTemplateResponse;
import com.example.admin.draft.domain.DraftFormTemplate;
import com.example.admin.draft.domain.DraftFormTemplateRoot;
import com.example.admin.draft.exception.DraftTemplateNotFoundException;
import com.example.admin.draft.repository.DraftFormTemplateRepository;
import com.example.admin.draft.repository.DraftFormTemplateRootRepository;

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

    /**
     * 템플릿 ID로 단건 조회한다.
     *
     * @param id 템플릿 ID
     * @return 템플릿 응답
     */
    @Transactional(readOnly = true)
    public DraftFormTemplateResponse findById(UUID id) {
        DraftFormTemplate template = draftFormTemplateRepository.findById(id)
                .orElseThrow(() -> new DraftTemplateNotFoundException("기안 양식 템플릿을 찾을 수 없습니다."));
        return DraftFormTemplateResponse.from(template);
    }

    /**
     * Root ID로 버전 히스토리를 조회한다.
     *
     * @param rootId 루트 ID
     * @return 버전 목록 (최신순)
     */
    @Transactional(readOnly = true)
    public List<DraftFormTemplateResponse> getVersionHistory(UUID rootId) {
        // Root 존재 확인
        if (!draftFormTemplateRootRepository.existsById(rootId)) {
            throw new DraftTemplateNotFoundException("기안 양식 템플릿 루트를 찾을 수 없습니다.");
        }

        return draftFormTemplateRepository.findAllByRootIdOrderByVersionDesc(rootId).stream()
                .map(DraftFormTemplateResponse::from)
                .toList();
    }

    /**
     * 특정 버전으로 롤백한다.
     * 지정된 버전의 내용을 복사하여 새 버전을 생성합니다.
     *
     * @param targetVersionId 롤백 대상 버전 ID
     * @param changeReason    롤백 사유
     * @param context         인증 컨텍스트
     * @param overwriteDraft  기존 초안 덮어쓰기 여부
     * @return 새로 생성된 버전
     */
    public DraftFormTemplateResponse rollbackToVersion(UUID targetVersionId,
                                                        String changeReason,
                                                        AuthContext context,
                                                        boolean overwriteDraft) {
        DraftFormTemplate targetVersion = draftFormTemplateRepository.findById(targetVersionId)
                .orElseThrow(() -> new DraftTemplateNotFoundException("롤백 대상 버전을 찾을 수 없습니다."));

        DraftFormTemplateRoot root = targetVersion.getRoot();

        // 초안이 존재하면서 덮어쓰기가 false인 경우 오류
        if (root.hasDraft() && !overwriteDraft) {
            throw new IllegalStateException("이미 초안 버전이 존재합니다. 덮어쓰려면 overwriteDraft=true를 지정하세요.");
        }

        // 기존 초안 삭제
        if (root.hasDraft()) {
            root.discardDraft();
        }

        OffsetDateTime now = OffsetDateTime.now();
        Integer maxVersion = draftFormTemplateRepository.findMaxVersionByRoot(root);
        int newVersion = (maxVersion != null ? maxVersion : 0) + 1;

        // 롤백 버전 생성 (대상 버전의 내용을 복사)
        DraftFormTemplate rollbackVersion = DraftFormTemplate.createFromRollback(
                root,
                newVersion,
                targetVersion.getName(),
                targetVersion.getWorkType(),
                targetVersion.getSchemaJson(),
                targetVersion.isActive(),
                targetVersion.getComponentPath(),
                changeReason != null ? changeReason : "버전 " + targetVersion.getVersion() + "으로 롤백",
                context.username(),
                context.username(),
                now,
                targetVersion.getVersion());
        draftFormTemplateRepository.save(rollbackVersion);

        // 현재 버전 종료 및 롤백 버전 활성화
        root.activateNewVersion(rollbackVersion, now);

        return DraftFormTemplateResponse.from(rollbackVersion);
    }

    /**
     * 템플릿을 삭제한다 (soft delete - active를 false로).
     *
     * @param rootId  루트 ID
     * @param context 인증 컨텍스트
     */
    public void deleteTemplate(UUID rootId, AuthContext context) {
        DraftFormTemplateRoot root = draftFormTemplateRootRepository.findById(rootId)
                .orElseThrow(() -> new DraftTemplateNotFoundException("기안 양식 템플릿을 찾을 수 없습니다."));

        DraftFormTemplate currentVersion = root.getCurrentVersion();
        if (currentVersion == null) {
            throw new IllegalStateException("현재 버전이 없습니다.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        Integer maxVersion = draftFormTemplateRepository.findMaxVersionByRoot(root);
        int newVersion = (maxVersion != null ? maxVersion : 0) + 1;

        // 비활성화된 새 버전 생성
        DraftFormTemplate deletedVersion = DraftFormTemplate.create(
                root,
                newVersion,
                currentVersion.getName(),
                currentVersion.getWorkType(),
                currentVersion.getSchemaJson(),
                false, // 비활성화
                currentVersion.getComponentPath(),
                ChangeAction.DELETE,
                "템플릿 삭제",
                context.username(),
                context.username(),
                now);
        draftFormTemplateRepository.save(deletedVersion);

        root.activateNewVersion(deletedVersion, now);
    }
}
