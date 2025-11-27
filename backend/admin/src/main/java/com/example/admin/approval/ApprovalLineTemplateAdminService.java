package com.example.admin.approval;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.admin.approval.dto.ApprovalLineTemplateRequest;
import com.example.admin.approval.dto.ApprovalLineTemplateResponse;
import com.example.admin.approval.dto.ApprovalTemplateStepRequest;
import com.example.admin.approval.dto.DisplayOrderUpdateRequest;
import com.example.admin.approval.dto.TemplateCopyRequest;
import com.example.admin.approval.dto.TemplateCopyResponse;
import com.example.admin.approval.dto.TemplateHistoryResponse;
import com.example.admin.approval.history.ApprovalLineTemplateHistory;
import com.example.admin.approval.history.ApprovalLineTemplateHistoryRepository;
import com.example.admin.permission.context.AuthContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 승인선 템플릿 관리 서비스 (v1 API용).
 */
@Service
@Transactional
public class ApprovalLineTemplateAdminService {

    private final ApprovalLineTemplateRepository templateRepository;
    private final ApprovalGroupRepository groupRepository;
    private final ApprovalLineTemplateHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;

    public ApprovalLineTemplateAdminService(ApprovalLineTemplateRepository templateRepository,
                                            ApprovalGroupRepository groupRepository,
                                            ApprovalLineTemplateHistoryRepository historyRepository,
                                            ObjectMapper objectMapper) {
        this.templateRepository = templateRepository;
        this.groupRepository = groupRepository;
        this.historyRepository = historyRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 승인선 템플릿 목록 조회 (페이징 없음).
     */
    @Transactional(readOnly = true)
    public List<ApprovalLineTemplateResponse> list(String keyword, boolean activeOnly) {
        return templateRepository.findAll().stream()
                .filter(t -> !activeOnly || t.isActive())
                .filter(t -> matchesKeyword(t, keyword))
                .sorted((a, b) -> {
                    int cmp = Integer.compare(a.getDisplayOrder(), b.getDisplayOrder());
                    return cmp != 0 ? cmp : a.getName().compareToIgnoreCase(b.getName());
                })
                .map(ApprovalLineTemplateResponse::from)
                .toList();
    }

    /**
     * 승인선 템플릿 단일 조회.
     */
    @Transactional(readOnly = true)
    public ApprovalLineTemplateResponse getById(UUID id) {
        ApprovalLineTemplate template = findTemplateOrThrow(id);
        return ApprovalLineTemplateResponse.from(template);
    }

    /**
     * 승인선 템플릿 생성.
     */
    public ApprovalLineTemplateResponse create(ApprovalLineTemplateRequest request, AuthContext context) {
        OffsetDateTime now = OffsetDateTime.now();

        ApprovalLineTemplate template = ApprovalLineTemplate.create(
                request.name(),
                request.displayOrder(),
                request.description(),
                now);

        addStepsToTemplate(template, request.steps());
        templateRepository.save(template);

        // 이력 기록
        String currentSnapshot = toJson(ApprovalLineTemplateResponse.from(template));
        ApprovalLineTemplateHistory history = ApprovalLineTemplateHistory.createHistory(
                template.getId(),
                context.username(),
                context.username(),
                now,
                currentSnapshot);
        historyRepository.save(history);

        return ApprovalLineTemplateResponse.from(template);
    }

    /**
     * 승인선 템플릿 수정.
     */
    public ApprovalLineTemplateResponse update(UUID id, ApprovalLineTemplateRequest request, AuthContext context) {
        ApprovalLineTemplate template = findTemplateOrThrow(id);
        OffsetDateTime now = OffsetDateTime.now();

        // 변경 전 상태 저장
        String previousSnapshot = toJson(ApprovalLineTemplateResponse.from(template));

        template.rename(request.name(), request.displayOrder(), request.description(), request.active(), now);
        template.getSteps().clear();
        addStepsToTemplate(template, request.steps());

        // 변경 후 상태 저장
        String currentSnapshot = toJson(ApprovalLineTemplateResponse.from(template));

        // 이력 기록
        ApprovalLineTemplateHistory history = ApprovalLineTemplateHistory.updateHistory(
                template.getId(),
                context.username(),
                context.username(),
                now,
                previousSnapshot,
                currentSnapshot);
        historyRepository.save(history);

        return ApprovalLineTemplateResponse.from(template);
    }

    /**
     * 승인선 템플릿 삭제 (soft delete - 비활성화).
     */
    public void delete(UUID id, AuthContext context) {
        ApprovalLineTemplate template = findTemplateOrThrow(id);
        OffsetDateTime now = OffsetDateTime.now();

        // 변경 전 상태 저장
        String previousSnapshot = toJson(ApprovalLineTemplateResponse.from(template));

        template.rename(template.getName(), template.getDisplayOrder(), template.getDescription(), false, now);

        // 변경 후 상태 저장
        String currentSnapshot = toJson(ApprovalLineTemplateResponse.from(template));

        // 이력 기록
        ApprovalLineTemplateHistory history = ApprovalLineTemplateHistory.deleteHistory(
                template.getId(),
                context.username(),
                context.username(),
                now,
                previousSnapshot,
                currentSnapshot);
        historyRepository.save(history);
    }

    /**
     * 승인선 템플릿 활성화 (복원).
     */
    public ApprovalLineTemplateResponse activate(UUID id, AuthContext context) {
        ApprovalLineTemplate template = findTemplateOrThrow(id);
        OffsetDateTime now = OffsetDateTime.now();

        // 변경 전 상태 저장
        String previousSnapshot = toJson(ApprovalLineTemplateResponse.from(template));

        template.rename(template.getName(), template.getDisplayOrder(), template.getDescription(), true, now);

        // 변경 후 상태 저장
        String currentSnapshot = toJson(ApprovalLineTemplateResponse.from(template));

        // 이력 기록
        ApprovalLineTemplateHistory history = ApprovalLineTemplateHistory.restoreHistory(
                template.getId(),
                context.username(),
                context.username(),
                now,
                previousSnapshot,
                currentSnapshot);
        historyRepository.save(history);

        return ApprovalLineTemplateResponse.from(template);
    }

    /**
     * 승인선 템플릿 복사.
     */
    public TemplateCopyResponse copy(UUID sourceId, TemplateCopyRequest request, AuthContext context) {
        ApprovalLineTemplate source = findTemplateOrThrow(sourceId);
        OffsetDateTime now = OffsetDateTime.now();

        // 새 템플릿 생성
        ApprovalLineTemplate copied = ApprovalLineTemplate.create(
                request.name(),
                source.getDisplayOrder(),
                request.description() != null ? request.description() : source.getDescription(),
                now);

        // 원본의 steps 복제
        for (ApprovalTemplateStep step : source.getSteps()) {
            copied.addStep(step.getStepOrder(), step.getApprovalGroup());
        }

        templateRepository.save(copied);

        // 이력 기록 (복사된 템플릿에 대한 이력)
        String currentSnapshot = toJson(ApprovalLineTemplateResponse.from(copied));
        ApprovalLineTemplateHistory history = ApprovalLineTemplateHistory.copyHistory(
                copied.getId(),
                context.username(),
                context.username(),
                now,
                currentSnapshot,
                sourceId);
        historyRepository.save(history);

        return TemplateCopyResponse.from(copied, source);
    }

    /**
     * 변경 이력 조회.
     */
    @Transactional(readOnly = true)
    public List<TemplateHistoryResponse> getHistory(UUID templateId) {
        // 템플릿 존재 여부 확인
        findTemplateOrThrow(templateId);

        return historyRepository.findByTemplateIdOrderByChangedAtDesc(templateId).stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    /**
     * 표시순서 일괄 변경.
     */
    public List<ApprovalLineTemplateResponse> updateDisplayOrders(DisplayOrderUpdateRequest request, AuthContext context) {
        OffsetDateTime now = OffsetDateTime.now();

        return request.items().stream()
                .map(item -> {
                    ApprovalLineTemplate template = findTemplateOrThrow(item.id());

                    // 변경 전 상태 저장
                    String previousSnapshot = toJson(ApprovalLineTemplateResponse.from(template));

                    template.rename(template.getName(), item.displayOrder(), template.getDescription(), template.isActive(), now);

                    // 변경 후 상태 저장
                    String currentSnapshot = toJson(ApprovalLineTemplateResponse.from(template));

                    // 이력 기록
                    ApprovalLineTemplateHistory history = ApprovalLineTemplateHistory.updateHistory(
                            template.getId(),
                            context.username(),
                            context.username(),
                            now,
                            previousSnapshot,
                            currentSnapshot);
                    historyRepository.save(history);

                    return ApprovalLineTemplateResponse.from(template);
                })
                .toList();
    }

    private ApprovalLineTemplate findTemplateOrThrow(UUID id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ApprovalLineTemplateNotFoundException("승인선 템플릿을 찾을 수 없습니다."));
    }

    private void addStepsToTemplate(ApprovalLineTemplate template, List<ApprovalTemplateStepRequest> stepRequests) {
        for (ApprovalTemplateStepRequest stepRequest : stepRequests) {
            ApprovalGroup group = groupRepository.findByGroupCode(stepRequest.approvalGroupCode())
                    .orElseThrow(() -> new ApprovalGroupNotFoundException(
                            "유효하지 않은 승인 그룹입니다: " + stepRequest.approvalGroupCode()));
            template.addStep(stepRequest.stepOrder(), group);
        }
    }

    private boolean matchesKeyword(ApprovalLineTemplate template, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String lowerKeyword = keyword.toLowerCase();
        return containsIgnoreCase(template.getName(), lowerKeyword)
                || containsIgnoreCase(template.getTemplateCode(), lowerKeyword)
                || containsIgnoreCase(template.getDescription(), lowerKeyword);
    }

    private boolean containsIgnoreCase(String target, String keyword) {
        if (target == null) {
            return false;
        }
        return target.toLowerCase().contains(keyword);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 직렬화 실패", e);
        }
    }

    private TemplateHistoryResponse toHistoryResponse(ApprovalLineTemplateHistory history) {
        Map<String, Object> changes = null;

        if (history.getPreviousSnapshot() != null || history.getCurrentSnapshot() != null) {
            changes = computeChanges(history.getPreviousSnapshot(), history.getCurrentSnapshot());
        }

        return new TemplateHistoryResponse(
                history.getId(),
                history.getAction().name(),
                history.getChangedBy(),
                history.getChangedByName(),
                history.getChangedAt(),
                changes);
    }

    private Map<String, Object> computeChanges(String previousJson, String currentJson) {
        try {
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> previous = previousJson != null
                    ? objectMapper.readValue(previousJson, new TypeReference<>() {})
                    : new HashMap<>();
            Map<String, Object> current = currentJson != null
                    ? objectMapper.readValue(currentJson, new TypeReference<>() {})
                    : new HashMap<>();

            // 변경된 필드만 추출
            for (String key : current.keySet()) {
                Object prevValue = previous.get(key);
                Object currValue = current.get(key);

                if (!objectsEqual(prevValue, currValue)) {
                    Map<String, Object> change = new HashMap<>();
                    change.put("before", prevValue);
                    change.put("after", currValue);
                    result.put(key, change);
                }
            }

            return result.isEmpty() ? null : result;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private boolean objectsEqual(Object a, Object b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }
}
