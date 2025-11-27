package com.example.approval.application;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.approval.api.ApprovalAction;
import com.example.approval.api.ApprovalStatus;
import com.example.admin.approval.ApprovalAccessDeniedException;
import com.example.approval.domain.ApprovalRequest;
import com.example.approval.domain.ApprovalStep;
import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountRepository;
import com.example.admin.permission.PermissionGroup;
import com.example.admin.permission.PermissionGroupRepository;

@Component
public class ApprovalAuthorizationService {

    private final PermissionGroupRepository permissionGroupRepository;
    private final UserAccountRepository userAccountRepository;

    public ApprovalAuthorizationService(PermissionGroupRepository permissionGroupRepository,
                                        UserAccountRepository userAccountRepository) {
        this.permissionGroupRepository = permissionGroupRepository;
        this.userAccountRepository = userAccountRepository;
    }

    public void ensureAuthorized(ApprovalRequest request,
                                 ApprovalAction action,
                                 String actor,
                                 String organizationCode) {
        // withdraw는 별도 검증 없음 (작성자/관리 권한은 상위 레이어에서 처리)
        if (action == ApprovalAction.WITHDRAW) {
            return;
        }

        String approvalGroupCode = resolveTargetGroupCode(request, action)
                .orElseThrow(() -> new ApprovalAccessDeniedException("활성 결재 단계를 찾을 수 없습니다."));

        List<PermissionGroup> permissionGroups =
                permissionGroupRepository.findByApprovalGroupCode(approvalGroupCode);

        if (permissionGroups.isEmpty()) {
            throw new ApprovalAccessDeniedException("결재 그룹에 매핑된 권한 그룹이 없습니다.");
        }

        List<String> groupCodes = permissionGroups.stream()
                .map(PermissionGroup::getCode)
                .toList();

        List<UserAccount> approvers =
                userAccountRepository.findByPermissionGroupCodeIn(groupCodes);

        boolean permitted = approvers.stream()
                .anyMatch(user -> user.getUsername().equals(actor)
                        && user.getOrganizationCode().equals(organizationCode));

        if (!permitted) {
            throw new ApprovalAccessDeniedException("결재 권한이 없습니다.");
        }
    }

    private Optional<String> resolveTargetGroupCode(ApprovalRequest request, ApprovalAction action) {
        if (action == ApprovalAction.DEFER_APPROVE) {
            return request.getSteps().stream()
                    .filter(step -> step.getStatus() == ApprovalStatus.DEFERRED)
                    .map(ApprovalStep::getApprovalGroupCode)
                    .findFirst();
        }

        return request.getSteps().stream()
                .filter(step -> step.getStatus() == ApprovalStatus.REQUESTED
                        || step.getStatus() == ApprovalStatus.IN_PROGRESS)
                .map(ApprovalStep::getApprovalGroupCode)
                .findFirst();
    }
}
