package com.example.admin.approval.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.example.admin.approval.domain.ApprovalGroup;
import com.example.admin.approval.exception.ApprovalGroupNotFoundException;
import com.example.admin.approval.repository.ApprovalGroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.admin.approval.dto.DisplayOrderUpdateRequest;
import com.example.admin.approval.dto.ApprovalGroupRequest;
import com.example.admin.approval.dto.ApprovalGroupResponse;
import com.example.admin.approval.dto.ApprovalGroupSummaryResponse;
import com.example.admin.approval.dto.ApprovalGroupUpdateRequest;
import com.example.admin.permission.context.AuthContext;

@Service
@Transactional
public class ApprovalGroupService {

    private final ApprovalGroupRepository approvalGroupRepository;

    public ApprovalGroupService(ApprovalGroupRepository approvalGroupRepository) {
        this.approvalGroupRepository = approvalGroupRepository;
    }

    public ApprovalGroupResponse createApprovalGroup(ApprovalGroupRequest request, AuthContext context, boolean audit) {
        OffsetDateTime now = OffsetDateTime.now();
        ApprovalGroup group = ApprovalGroup.create(
                request.groupCode(),
                request.name(),
                request.description(),
                request.displayOrder() != null ? request.displayOrder() : 0,
                now);
        approvalGroupRepository.save(group);
        return ApprovalGroupResponse.from(group);
    }

    @Transactional(readOnly = true)
    public ApprovalGroupResponse getApprovalGroup(UUID id) {
        ApprovalGroup group = approvalGroupRepository.findById(id)
                .orElseThrow(() -> new ApprovalGroupNotFoundException("결재 그룹을 찾을 수 없습니다."));
        return ApprovalGroupResponse.from(group);
    }

    public ApprovalGroupResponse updateApprovalGroup(UUID id, ApprovalGroupUpdateRequest request, AuthContext context, boolean audit) {
        ApprovalGroup group = approvalGroupRepository.findById(id)
                .orElseThrow(() -> new ApprovalGroupNotFoundException("결재 그룹을 찾을 수 없습니다."));
        OffsetDateTime now = OffsetDateTime.now();
        group.rename(request.name(), request.description(), now);
        if (request.displayOrder() != null) {
            group.updateDisplayOrder(request.displayOrder(), now);
        }
        return ApprovalGroupResponse.from(group);
    }

    public void deleteApprovalGroup(UUID id, AuthContext context, boolean audit) {
        ApprovalGroup group = approvalGroupRepository.findById(id)
                .orElseThrow(() -> new ApprovalGroupNotFoundException("결재 그룹을 찾을 수 없습니다."));
        group.deactivate(OffsetDateTime.now());
    }

    public ApprovalGroupResponse activateApprovalGroup(UUID id, AuthContext context, boolean audit) {
        ApprovalGroup group = approvalGroupRepository.findById(id)
                .orElseThrow(() -> new ApprovalGroupNotFoundException("결재 그룹을 찾을 수 없습니다."));
        group.activate(OffsetDateTime.now());
        return ApprovalGroupResponse.from(group);
    }

    @Transactional(readOnly = true)
    public List<ApprovalGroupResponse> listApprovalGroups(String keyword, boolean activeOnly, AuthContext context, boolean audit) {
        return approvalGroupRepository.findAll().stream()
                .filter(g -> !activeOnly || g.isActive())
                .filter(g -> keyword == null || keyword.isBlank()
                        || containsIgnoreCase(g.getGroupCode(), keyword)
                        || containsIgnoreCase(g.getName(), keyword)
                        || containsIgnoreCase(g.getDescription(), keyword))
                .sorted((a, b) -> {
                    int cmp = Integer.compare(a.getDisplayOrder(), b.getDisplayOrder());
                    return cmp != 0 ? cmp : a.getName().compareToIgnoreCase(b.getName());
                })
                .map(ApprovalGroupResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean existsGroupCode(String groupCode) {
        return approvalGroupRepository.existsByGroupCode(groupCode);
    }

    public List<ApprovalGroupResponse> updateApprovalGroupDisplayOrders(DisplayOrderUpdateRequest request, AuthContext context, boolean audit) {
        OffsetDateTime now = OffsetDateTime.now();
        return request.items().stream()
                .map(item -> {
                    ApprovalGroup group = approvalGroupRepository.findById(item.id())
                            .orElseThrow(() -> new ApprovalGroupNotFoundException("결재 그룹을 찾을 수 없습니다: " + item.id()));
                    group.updateDisplayOrder(item.displayOrder(), now);
                    return ApprovalGroupResponse.from(group);
                })
                .toList();
    }

    /**
     * 승인그룹 요약 목록 조회.
     */
    @Transactional(readOnly = true)
    public List<ApprovalGroupSummaryResponse> listGroupSummary(boolean activeOnly) {
        return approvalGroupRepository.findAll().stream()
                .filter(g -> !activeOnly || g.isActive())
                .sorted((a, b) -> {
                    int cmp = Integer.compare(a.getDisplayOrder(), b.getDisplayOrder());
                    return cmp != 0 ? cmp : a.getName().compareToIgnoreCase(b.getName());
                })
                .map(ApprovalGroupSummaryResponse::from)
                .toList();
    }

    private boolean containsIgnoreCase(String target, String keyword) {
        if (target == null) {
            return false;
        }
        return target.toLowerCase().contains(keyword.toLowerCase());
    }
}
